package jp.momiji.feature.query.order.listmyorders

import iss.jooq.generated.tables.references.ORDERS
import iss.jooq.generated.tables.references.ORDER_ITEMS
import jp.momiji.feature.query.Page
import jp.momiji.feature.query.Paging
import jp.momiji.feature.query.PagingCondition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ListMyOrdersQueryService(
    private val dsl: DSLContext,
) {
    /**
     * [userId] の注文を新しい順（直近の注文を先頭）でページングして返す。
     * 合計金額・明細は注文時点スナップショット（order_items）から復元する。
     *
     * [createdFrom]/[createdTo] は注文日時（created_at）の範囲フィルタ（半開区間 `[from, to)`）。
     * 期間の意味づけ（「直近1か月」「2025年」等）は BFF が解決して絶対時刻で渡す。 null の側は開放する。
     */
    fun findByUserId(
        userId: String,
        paging: PagingCondition,
        createdFrom: LocalDateTime? = null,
        createdTo: LocalDateTime? = null,
    ): Page<MyOrderView> {
        // 総件数はウィンドウ関数 count() over() で 1 クエリにまとめて取る（別 count クエリを撃たない）。
        val totalCountField = DSL.count().over()

        // 期間フィルタ: null の側は noCondition で開放（片側のみ・両側なし も同じ書き方で表現できる）。
        val fromCondition = createdFrom?.let { ORDERS.CREATED_AT.ge(it) } ?: DSL.noCondition()
        val toCondition = createdTo?.let { ORDERS.CREATED_AT.lt(it) } ?: DSL.noCondition()

        val orders =
            dsl
                .select(
                    ORDERS.ID,
                    ORDERS.STATUS,
                    ORDERS.CREATED_AT,
                    totalCountField,
                ).from(ORDERS)
                .where(ORDERS.USER_ID.eq(userId).and(fromCondition).and(toCondition))
                .orderBy(ORDERS.CREATED_AT.desc())
                .limit(paging.pageSize)
                .offset(paging.offset)
                .fetch()

        val totalCount = orders.firstOrNull()?.get(totalCountField)?.toLong() ?: 0L

        // 明細は対象ページの注文 ID をまとめて 1 クエリで引く（注文ごとの round trip を避ける）。
        val orderIds = orders.map { it[ORDERS.ID]!! }
        val itemsByOrder =
            if (orderIds.isEmpty()) {
                emptyMap()
            } else {
                dsl
                    .select(
                        ORDER_ITEMS.ORDER_ID,
                        ORDER_ITEMS.PRODUCT_ID,
                        ORDER_ITEMS.NAME,
                        ORDER_ITEMS.UNIT_PRICE,
                        ORDER_ITEMS.QUANTITY,
                        ORDER_ITEMS.IMAGE_URL,
                    ).from(ORDER_ITEMS)
                    .where(ORDER_ITEMS.ORDER_ID.`in`(orderIds))
                    .fetchGroups(ORDER_ITEMS.ORDER_ID)
            }

        val views =
            orders.map { order ->
                val orderId = order[ORDERS.ID]!!
                val records = itemsByOrder[orderId].orEmpty()
                val items =
                    records.map { record ->
                        MyOrderView.Item(
                            productId = record[ORDER_ITEMS.PRODUCT_ID]!!,
                            name = record[ORDER_ITEMS.NAME]!!,
                            unitPrice = record[ORDER_ITEMS.UNIT_PRICE]!!,
                            quantity = record[ORDER_ITEMS.QUANTITY]!!,
                            imageUrl = record[ORDER_ITEMS.IMAGE_URL],
                        )
                    }
                val totalAmount =
                    records.sumOf { record -> record[ORDER_ITEMS.UNIT_PRICE]!!.toLong() * record[ORDER_ITEMS.QUANTITY]!! }
                MyOrderView(
                    orderId = orderId,
                    status = order[ORDERS.STATUS]!!,
                    totalAmount = totalAmount,
                    createdAt = order[ORDERS.CREATED_AT]!!,
                    items = items,
                )
            }

        return Page(
            items = views,
            paging =
                Paging(
                    totalCount = totalCount,
                    pageSize = paging.pageSize,
                    pageNumber = paging.pageNumber,
                ),
        )
    }
}
