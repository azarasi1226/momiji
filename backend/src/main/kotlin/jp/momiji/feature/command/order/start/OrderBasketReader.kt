package jp.momiji.feature.command.order.start

import iss.jooq.generated.tables.references.BASKETS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

/**
 * 注文開始のためにカート（baskets）の中身を読み、 注文明細（[StartOrderCommand.Item]）にする read model リーダー。
 *
 * カートの内容は **server 側で読む**（金額・個数はクライアントを信用しない）。 CommandHandler 内で user_id から
 * カートを復元できないのは、 @EventCriteriaBuilder に渡す product_id が手前で必要だから（DCB の整合境界に
 * product を入れるため）。 見ていた画面とカートが食い違っても、 expectedTotalAmount で CommandHandler が弾く。
 */
@Component
class OrderBasketReader(
    private val dsl: DSLContext,
) {
    fun readItems(userId: String): List<StartOrderCommand.Item> =
        dsl
            .select(BASKETS.PRODUCT_ID, BASKETS.ITEM_QUANTITY)
            .from(BASKETS)
            .where(BASKETS.USER_ID.eq(userId))
            .orderBy(BASKETS.ADDED_AT.asc())
            .fetch()
            .map { record ->
                StartOrderCommand.Item(
                    productId = record.value1()!!,
                    quantity = record.value2()!!,
                )
            }
}
