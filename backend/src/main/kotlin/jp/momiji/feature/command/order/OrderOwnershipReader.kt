package jp.momiji.feature.command.order

import iss.jooq.generated.tables.references.ORDERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

/**
 * 注文が指定ユーザーの所有かを read model（orders.user_id）で判定する共有リーダー。
 *
 * ユーザー起点の注文コマンド（キャンセル等）の **認可** に使う。 [OrderState] は user_id を持たない（order_id 境界）ため
 * 所有権は CommandHandler では検証できず、 ここ（routing 層）で確認する。 user_id は不変なので projection ラグの影響を受けない
 * （PayableOrderReader が決済の所有確認を read model で行うのと同じ判断）。
 */
@Component
class OrderOwnershipReader(
    private val dsl: DSLContext,
) {
    fun isOwnedBy(
        orderId: String,
        userId: String,
    ): Boolean =
        dsl.fetchExists(
            dsl
                .selectFrom(ORDERS)
                .where(ORDERS.ID.eq(orderId).and(ORDERS.USER_ID.eq(userId))),
        )
}
