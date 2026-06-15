package jp.momiji.feature.command.order.preparepayment

import iss.jooq.generated.tables.references.ORDERS
import iss.jooq.generated.tables.references.ORDER_ITEMS
import iss.jooq.generated.tables.references.PAYMENT_METHODS
import iss.jooq.generated.tables.references.USERS
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.domain.order.OrderStatus
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

/**
 * 決済準備の前提条件（注文・カード・Customer・合計金額）を read model から確認し、 PaymentIntent 作成に必要な情報（[PayableOrder]）を返す。
 * 条件を満たさない場合は [BusinessException]。
 */
@Component
class PayableOrderReader(
    private val dsl: DSLContext,
) {
    /**
     * 下記内容を検証し、 customer_id と 合計金額を返却する。
     * * [orderId] が [userId] のもので決済可能（STARTED/PAYMENT_PENDING）か
     * * [paymentMethodId] が本人のカードか
     */
    fun loadForPayment(
        orderId: String,
        userId: String,
        paymentMethodId: String,
    ): PayableOrder {
        // 　本人確認
        requireOwnedAndPayable(orderId, userId)
        requireCardOwned(paymentMethodId, userId)

        return PayableOrder(
            stripeCustomerId = requireStripeCustomerId(userId),
            totalAmount = requireTotalAmount(orderId),
        )
    }

    // 注文が本人のもので、 まだ決済可能（STARTED/PAYMENT_PENDING）か？
    private fun requireOwnedAndPayable(
        orderId: String,
        userId: String,
    ) {
        val order =
            dsl
                .select(ORDERS.USER_ID, ORDERS.STATUS)
                .from(ORDERS)
                .where(ORDERS.ID.eq(orderId))
                .fetchOne()
        if (order == null || order[ORDERS.USER_ID] != userId) {
            throw BusinessException(BusinessError("注文が見つかりません"))
        }
        val status = order[ORDERS.STATUS]
        if (status != OrderStatus.STARTED.name && status != OrderStatus.PAYMENT_PENDING.name) {
            throw BusinessException(BusinessError("この注文は決済できません"))
        }
    }

    // paymentMethodIdはそのユーザーによるものか？
    private fun requireCardOwned(
        paymentMethodId: String,
        userId: String,
    ) {
        val owned =
            dsl.fetchExists(
                dsl
                    .selectFrom(PAYMENT_METHODS)
                    .where(PAYMENT_METHODS.ID.eq(paymentMethodId).and(PAYMENT_METHODS.USER_ID.eq(userId))),
            )
        if (!owned) {
            throw BusinessException(BusinessError("カードが見つかりません"))
        }
    }

    // customerIdを解決
    private fun requireStripeCustomerId(userId: String): String =
        dsl
            .select(USERS.STRIPE_CUSTOMER_ID)
            .from(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOne(USERS.STRIPE_CUSTOMER_ID)
            ?: throw BusinessException(BusinessError("決済情報が未登録です"))

    // 合計金額は注文時点スナップショット（order_items）から復元する。
    private fun requireTotalAmount(orderId: String): Long =
        dsl
            .select(DSL.sum(ORDER_ITEMS.UNIT_PRICE.mul(ORDER_ITEMS.QUANTITY)))
            .from(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetchOne()
            ?.value1()
            ?.toLong()
            ?: throw BusinessException(BusinessError("注文明細がありません"))
}

/** 決済準備で PaymentIntent を作るのに要る情報。 */
data class PayableOrder(
    val stripeCustomerId: String,
    val totalAmount: Long,
)
