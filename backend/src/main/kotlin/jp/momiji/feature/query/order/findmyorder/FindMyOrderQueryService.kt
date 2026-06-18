package jp.momiji.feature.query.order.findmyorder

import iss.jooq.generated.tables.references.ORDERS
import iss.jooq.generated.tables.references.ORDER_ITEMS
import iss.jooq.generated.tables.references.PAYMENT_METHODS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class FindMyOrderQueryService(
    private val dsl: DSLContext,
) {
    /**
     * [orderId] の注文詳細を返す。 **[userId] 本人の注文でなければ null**（他人の注文を覗かせない）。
     * 配送先・合計・明細は注文時点スナップショットから復元。 使用カードは payment_methods を LEFT JOIN
     * （未着手で未設定／後でカード削除済みなら null）。
     */
    fun findByIdForUser(
        orderId: String,
        userId: String,
    ): MyOrderDetailView? {
        val order =
            dsl
                .select(
                    ORDERS.STATUS,
                    ORDERS.CREATED_AT,
                    ORDERS.UPDATED_AT,
                    ORDERS.RECIPIENT_NAME,
                    ORDERS.PHONE_NUMBER,
                    ORDERS.POSTAL_CODE,
                    ORDERS.PREFECTURE,
                    ORDERS.CITY,
                    ORDERS.STREET_ADDRESS,
                    ORDERS.BUILDING,
                    ORDERS.DELIVERY_NOTE,
                    PAYMENT_METHODS.ID,
                    PAYMENT_METHODS.BRAND,
                    PAYMENT_METHODS.LAST4,
                ).from(ORDERS)
                // 使用カードは別 read model。 未設定／削除済みもあるので LEFT JOIN。
                .leftJoin(PAYMENT_METHODS)
                .on(PAYMENT_METHODS.ID.eq(ORDERS.PAYMENT_METHOD_ID))
                // 本人の注文だけ（ownership は read model の user_id で確認。 user_id は不変なのでラグの影響を受けない）。
                .where(ORDERS.ID.eq(orderId).and(ORDERS.USER_ID.eq(userId)))
                .fetchOne()
                ?: return null

        val items =
            dsl
                .select(
                    ORDER_ITEMS.PRODUCT_ID,
                    ORDER_ITEMS.NAME,
                    ORDER_ITEMS.UNIT_PRICE,
                    ORDER_ITEMS.QUANTITY,
                    ORDER_ITEMS.IMAGE_URL,
                ).from(ORDER_ITEMS)
                .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
                .fetch { record ->
                    MyOrderDetailView.Item(
                        productId = record[ORDER_ITEMS.PRODUCT_ID]!!,
                        name = record[ORDER_ITEMS.NAME]!!,
                        unitPrice = record[ORDER_ITEMS.UNIT_PRICE]!!,
                        quantity = record[ORDER_ITEMS.QUANTITY]!!,
                        imageUrl = record[ORDER_ITEMS.IMAGE_URL],
                    )
                }
        val totalAmount = items.sumOf { it.unitPrice.toLong() * it.quantity }

        // カードは LEFT JOIN なので、 join が当たったとき（id 非 null）だけ詰める。
        val paymentMethod =
            order[PAYMENT_METHODS.ID]?.let { id ->
                MyOrderDetailView.PaymentMethod(
                    id = id,
                    brand = order[PAYMENT_METHODS.BRAND]!!,
                    last4 = order[PAYMENT_METHODS.LAST4]!!,
                )
            }

        return MyOrderDetailView(
            orderId = orderId,
            status = order[ORDERS.STATUS]!!,
            totalAmount = totalAmount,
            createdAt = order[ORDERS.CREATED_AT]!!,
            updatedAt = order[ORDERS.UPDATED_AT]!!,
            shippingAddress =
                MyOrderDetailView.ShippingAddress(
                    recipientName = order[ORDERS.RECIPIENT_NAME]!!,
                    phoneNumber = order[ORDERS.PHONE_NUMBER]!!,
                    postalCode = order[ORDERS.POSTAL_CODE]!!,
                    prefecture = order[ORDERS.PREFECTURE]!!,
                    city = order[ORDERS.CITY]!!,
                    streetAddress = order[ORDERS.STREET_ADDRESS]!!,
                    building = order[ORDERS.BUILDING]!!,
                    deliveryNote = order[ORDERS.DELIVERY_NOTE]!!,
                ),
            paymentMethod = paymentMethod,
            items = items,
        )
    }
}
