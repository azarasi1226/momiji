package jp.momiji.projection.order

import iss.jooq.generated.tables.references.ORDERS
import iss.jooq.generated.tables.references.ORDER_ITEMS
import jp.momiji.domain.order.OrderStatus
import jp.momiji.event.order.OrderStartedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 注文 ReadModel（orders / order_items）を更新する projection。
 *
 * **注文時点のスナップショット**（配送先・商品名・単価）を焼き込むので、 後で商品が廃番・改価・リネームされても、
 * 配送先が編集・削除されても、 **注文の内容は永久に残る**（products / shipping_addresses を参照しない自己完結）。
 */
@Component
class OrderTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: OrderStartedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC)
        val address = event.shippingAddress
        // 冪等性: イベント再処理で同じ order_id が来ても二重 insert しない。
        dsl
            .insertInto(ORDERS)
            .set(ORDERS.ID, event.orderId)
            .set(ORDERS.USER_ID, event.userId)
            .set(ORDERS.STATUS, OrderStatus.STARTED.name)
            // payment_method_id は決済フェーズで埋める（注文開始時点は NULL のまま）。
            .set(ORDERS.RECIPIENT_NAME, address.name)
            .set(ORDERS.PHONE_NUMBER, address.phoneNumber)
            .set(ORDERS.POSTAL_CODE, address.postalCode)
            .set(ORDERS.PREFECTURE, address.prefecture)
            .set(ORDERS.CITY, address.city)
            .set(ORDERS.STREET_ADDRESS, address.streetAddress)
            .set(ORDERS.BUILDING, address.building)
            .set(ORDERS.DELIVERY_NOTE, address.deliveryNote)
            .set(ORDERS.CREATED_AT, at)
            .set(ORDERS.UPDATED_AT, at)
            .onDuplicateKeyIgnore()
            .execute()

        // 明細は 1 バッチでまとめて insert（N 回の round trip を 1 回に）。 各行は冪等（onDuplicateKeyIgnore）。
        dsl
            .batch(
                event.items.map { item ->
                    dsl
                        .insertInto(ORDER_ITEMS)
                        .set(ORDER_ITEMS.ORDER_ID, event.orderId)
                        .set(ORDER_ITEMS.PRODUCT_ID, item.productId)
                        .set(ORDER_ITEMS.NAME, item.name)
                        .set(ORDER_ITEMS.UNIT_PRICE, item.unitPrice)
                        .set(ORDER_ITEMS.QUANTITY, item.quantity)
                        .onDuplicateKeyIgnore()
                },
            ).execute()
    }
}
