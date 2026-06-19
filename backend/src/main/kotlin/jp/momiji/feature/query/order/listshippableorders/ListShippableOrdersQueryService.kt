package jp.momiji.feature.query.order.listshippableorders

import iss.jooq.generated.tables.references.ORDERS
import iss.jooq.generated.tables.references.ORDER_ITEMS
import jp.momiji.domain.order.OrderStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ListShippableOrdersQueryService(
    private val dsl: DSLContext,
) {
    /** 発送待ち（PAID）の注文を、 古い順（先に来た注文を先に発送）で返す。 合計金額は注文時点スナップショット（order_items）から復元。 */
    fun findShippable(): List<ShippableOrderView> {
        val orders =
            dsl
                .select(
                    ORDERS.ID,
                    ORDERS.RECIPIENT_NAME,
                    ORDERS.PHONE_NUMBER,
                    ORDERS.POSTAL_CODE,
                    ORDERS.PREFECTURE,
                    ORDERS.CITY,
                    ORDERS.STREET_ADDRESS,
                    ORDERS.BUILDING,
                    ORDERS.DELIVERY_NOTE,
                    ORDERS.CREATED_AT,
                ).from(ORDERS)
                .where(ORDERS.STATUS.eq(OrderStatus.PAID.name))
                .orderBy(ORDERS.CREATED_AT.asc())
                .fetch()
        if (orders.isEmpty()) return emptyList()

        val orderIds = orders.map { it[ORDERS.ID]!! }
        val itemsByOrder =
            dsl
                .select(ORDER_ITEMS.ORDER_ID, ORDER_ITEMS.NAME, ORDER_ITEMS.UNIT_PRICE, ORDER_ITEMS.QUANTITY)
                .from(ORDER_ITEMS)
                .where(ORDER_ITEMS.ORDER_ID.`in`(orderIds))
                .fetchGroups(ORDER_ITEMS.ORDER_ID)

        return orders.map { order ->
            val orderId = order[ORDERS.ID]!!
            val records = itemsByOrder[orderId].orEmpty()
            val items =
                records.map { record ->
                    ShippableOrderView.Item(name = record[ORDER_ITEMS.NAME]!!, quantity = record[ORDER_ITEMS.QUANTITY]!!)
                }
            val totalAmount =
                records.sumOf { record -> record[ORDER_ITEMS.UNIT_PRICE]!!.toLong() * record[ORDER_ITEMS.QUANTITY]!! }
            ShippableOrderView(
                orderId = orderId,
                shippingAddress =
                    ShippableOrderView.ShippingAddress(
                        recipientName = order[ORDERS.RECIPIENT_NAME]!!,
                        phoneNumber = order[ORDERS.PHONE_NUMBER]!!,
                        postalCode = order[ORDERS.POSTAL_CODE]!!,
                        prefecture = order[ORDERS.PREFECTURE]!!,
                        city = order[ORDERS.CITY]!!,
                        streetAddress = order[ORDERS.STREET_ADDRESS]!!,
                        building = order[ORDERS.BUILDING]!!,
                        deliveryNote = order[ORDERS.DELIVERY_NOTE]!!,
                    ),
                totalAmount = totalAmount,
                createdAt = order[ORDERS.CREATED_AT]!!,
                items = items,
            )
        }
    }
}
