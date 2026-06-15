package jp.momiji.feature.command.order.start

import jp.momiji.domain.product.ProductStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.product.ProductUpdatedEvent
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.event.stock.StockReservedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/** [ProductsState] の id。 注文対象の全 product_id（@InjectEntity(idProperty = "productIds") で解決）。 */
data class OrderProductIds(
    val values: List<String>,
)

/**
 * product_id（×n）境界の DCB State。 注文対象の各商品の状態（ACTIVE/DISCONTINUED）・名前・単価・在庫・予約を、
 * productId をキーに 1 商品 1 オブジェクトで持つ。 N 商品の在庫ストリームを 1 整合境界に入れ、 atomic な
 * append 条件（楽観排他）で oversell を防ぐ。
 *
 * 在庫イベントが商品作成より先に来ても破綻しないよう get-or-create する（status/name/price は nullable）。
 */
@EventSourced(idType = OrderProductIds::class)
class ProductsState private constructor(
    private val byId: MutableMap<String, Product>,
) {
    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        private fun resolveCriteria(id: OrderProductIds): EventCriteria {
            // 空注文（service 層で弾く想定の防御）。 実在しない product_id="" で何もソースしない criteria。
            if (id.values.isEmpty()) {
                return EventCriteria
                    .havingTags(Tag.of(MomijiEventTag.PRODUCT_ID, ""))
                    .andBeingOneOfTypes(ProductCreatedEvent::class.eventQualifiedName())
            }
            return EventCriteria.either(
                id.values.map { productId ->
                    EventCriteria
                        .havingTags(Tag.of(MomijiEventTag.PRODUCT_ID, productId))
                        .andBeingOneOfTypes(
                            ProductCreatedEvent::class.eventQualifiedName(),
                            ProductUpdatedEvent::class.eventQualifiedName(),
                            ProductDiscontinuedEvent::class.eventQualifiedName(),
                            StockReceivedEvent::class.eventQualifiedName(),
                            StockAdjustedEvent::class.eventQualifiedName(),
                            StockReservedEvent::class.eventQualifiedName(),
                            StockReservationReleasedEvent::class.eventQualifiedName(),
                        )
                },
            )
        }
    }

    @EntityCreator
    constructor() : this(mutableMapOf())

    fun isActive(productId: String): Boolean = byId[productId]?.status == ProductStatus.ACTIVE

    fun available(productId: String): Int = byId[productId]?.available ?: 0

    fun reservedOf(productId: String): Int = byId[productId]?.reserved ?: 0

    fun nameOf(productId: String): String = requireNotNull(byId.getValue(productId).name)

    fun priceOf(productId: String): Int = requireNotNull(byId.getValue(productId).price)

    fun imageUrlOf(productId: String): String? = byId.getValue(productId).imageUrl

    private fun getOrCreate(productId: String): Product = byId.getOrPut(productId) { Product() }

    @EventSourcingHandler
    fun evolve(event: ProductCreatedEvent) {
        getOrCreate(event.id).apply {
            status = ProductStatus.ACTIVE
            name = event.name
            price = event.price
            imageUrl = event.imageUrl
        }
    }

    @EventSourcingHandler
    fun evolve(event: ProductUpdatedEvent) {
        // 名前・価格・画像の変更を反映（在庫・status は対象外）。 注文時点の最新値をスナップショットに使う。
        getOrCreate(event.id).apply {
            name = event.name
            price = event.price
            imageUrl = event.imageUrl
        }
    }

    @EventSourcingHandler
    fun evolve(event: ProductDiscontinuedEvent) {
        getOrCreate(event.id).status = ProductStatus.DISCONTINUED
    }

    @EventSourcingHandler
    fun evolve(event: StockReceivedEvent) {
        getOrCreate(event.productId).onHand = event.onHandQuantity
    }

    @EventSourcingHandler
    fun evolve(event: StockAdjustedEvent) {
        getOrCreate(event.productId).onHand = event.onHandQuantity
    }

    @EventSourcingHandler
    fun evolve(event: StockReservedEvent) {
        getOrCreate(event.productId).reserved += event.quantity
    }

    @EventSourcingHandler
    fun evolve(event: StockReservationReleasedEvent) {
        getOrCreate(event.productId).reserved -= event.quantity
    }

    private class Product(
        var status: ProductStatus? = null,
        var name: String? = null,
        var price: Int? = null,
        var imageUrl: String? = null,
        var onHand: Int = 0,
        var reserved: Int = 0,
    ) {
        val available: Int get() = onHand - reserved
    }
}
