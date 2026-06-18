package jp.momiji.feature.command.stock

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationCommittedEvent
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced

/**
 * product_id タグの在庫 State。 商品存在（ProductCreated）と物理在庫（[onHand]）を持つ。
 * 入庫（ReceiveStock）・棚卸し調整（AdjustStock）が resulting onHand を算出する整合境界として**共有**する。
 * 在庫は最初の入庫まで暗黙ゼロ（イベントが無ければ onHand=0）。
 *
 * onHand を動かすイベント（入庫・調整・出荷確定）をすべて畳む。 新しく onHand を動かすイベントを足したら、
 * ここに evolve を追加する（receive/adjust 双方に効く）。
 */
@EventSourced(tagKey = MomijiEventTag.PRODUCT_ID, idType = String::class)
class ProductStockState(
    var productExists: Boolean,
    var onHand: Int,
) {
    @EntityCreator
    constructor() : this(
        productExists = false,
        onHand = 0,
    )

    @EventSourcingHandler
    fun evolve(event: ProductCreatedEvent) {
        productExists = true
    }

    // ProductDiscontinuedEventは考慮しない。
    // 机上の商品が消された瞬間に、物理的な在庫も消滅するなんてあり得る?
    // だから Stock と Product は独立していると考える。

    @EventSourcingHandler
    fun evolve(event: StockReceivedEvent) {
        onHand = event.onHandQuantity
    }

    @EventSourcingHandler
    fun evolve(event: StockAdjustedEvent) {
        onHand = event.onHandQuantity
    }

    // 出荷確定（注文完了）でも onHand は減る。 入庫・調整の resulting 計算が古い在庫にならないよう source する。
    @EventSourcingHandler
    fun evolve(event: StockReservationCommittedEvent) {
        onHand = event.onHandQuantity
    }
}
