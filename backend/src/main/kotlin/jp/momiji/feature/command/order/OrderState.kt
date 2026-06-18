package jp.momiji.feature.command.order

import jp.momiji.domain.order.OrderStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.order.OrderCompletedEvent
import jp.momiji.event.order.OrderFailedEvent
import jp.momiji.event.order.OrderPaidEvent
import jp.momiji.event.order.OrderPaymentPreparedEvent
import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.event.order.OrderStartedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * order_id 境界の DCB State。 注文のライフサイクル状態（[status]）・予約した商品（[reservedItems]）・決済 PaymentIntent（[paymentIntentId]）を見る。
 *
 * - StartOrder: 既に開始済みか（[notStarted]）の冪等判定。
 * - 決済準備: STARTED のとき PAYMENT_PENDING へ遷移（pi_ 記録）。
 * - 決済成功: PAYMENT_PENDING のとき PAID へ。
 * - 発送: PAID のとき SHIPPED へ（admin の発送手続き）。
 * - 完了: SHIPPED のとき COMPLETED へ（発送を拾った reactor が自動で）。
 * - 失敗/期限切れ: [canReleaseReservation]（STARTED or PAYMENT_PENDING）のとき [reservedItems] の在庫予約を解放。
 *
 * @InjectEntity(idProperty = ...) で order_id を渡して解決される。
 */
@EventSourced(idType = String::class)
class OrderState(
    var status: OrderStatus?,
    // 予約された商品（OrderStarted のスナップショット由来）。 失効/失敗時の解放対象。
    val reservedItems: MutableList<ReservedItem>,
    // 決済の PaymentIntent（pi_）。 決済準備で記録。 期限切れ後課金の返金などに使う。
    var paymentIntentId: String?,
    // 注文時点スナップショットの権威合計（OrderStarted の単価×個数）。 決済準備で課金額の検証に使う。
    var totalAmount: Long,
) {
    data class ReservedItem(
        val productId: String,
        val quantity: Int,
    )

    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        private fun resolveCriteria(orderId: String): EventCriteria =
            EventCriteria
                .havingTags(Tag.of(MomijiEventTag.ORDER_ID, orderId))
                .andBeingOneOfTypes(
                    OrderStartedEvent::class.eventQualifiedName(),
                    OrderPaymentPreparedEvent::class.eventQualifiedName(),
                    OrderPaidEvent::class.eventQualifiedName(),
                    OrderShippedEvent::class.eventQualifiedName(),
                    OrderCompletedEvent::class.eventQualifiedName(),
                    OrderFailedEvent::class.eventQualifiedName(),
                )
    }

    @EntityCreator
    constructor() : this(status = null, reservedItems = mutableListOf(), paymentIntentId = null, totalAmount = 0)

    /** まだ注文が開始されていない（この order_id のイベントが無い）。 StartOrder の冪等判定に使う。 */
    val notStarted: Boolean get() = status == null

    /** STARTED（予約済み・決済未着手）。 決済準備はこの状態のときだけ PaymentIntent を記録する。 */
    val isStarted: Boolean get() = status == OrderStatus.STARTED

    /** PAYMENT_PENDING（決済着手済み・確定待ち）。 決済成功はこの状態のときだけ PAID にする。 */
    val isPaymentPending: Boolean get() = status == OrderStatus.PAYMENT_PENDING

    /** 在庫予約を解放できる（＝まだ確定していない STARTED/PAYMENT_PENDING）状態か。 失効・支払い失敗の補償はこのときだけ解放する。 */
    val canReleaseReservation: Boolean get() = status == OrderStatus.STARTED || status == OrderStatus.PAYMENT_PENDING

    /** 既に決済済み or それ以降（PAID/SHIPPED/COMPLETED）。 決済成功 webhook の再送・進行後の重複を冪等に握る（返金しない）ために使う。 */
    val isPaidOrBeyond: Boolean
        get() = status == OrderStatus.PAID || status == OrderStatus.SHIPPED || status == OrderStatus.COMPLETED

    /** PAID（決済確定・未発送）。 発送手続きはこの状態のときだけ SHIPPED にする。 */
    val isPaid: Boolean get() = status == OrderStatus.PAID

    /** 既に発送済み or それ以降（SHIPPED/COMPLETED）。 発送手続きの重複を冪等に握るために使う。 */
    val isShippedOrBeyond: Boolean get() = status == OrderStatus.SHIPPED || status == OrderStatus.COMPLETED

    /** SHIPPED（発送済み・未完了）。 完了手続きはこの状態のときだけ COMPLETED にする。 */
    val isShipped: Boolean get() = status == OrderStatus.SHIPPED

    /** COMPLETED（完了・終端）。 完了手続きの重複を冪等に握るために使う。 */
    val isCompleted: Boolean get() = status == OrderStatus.COMPLETED

    /**
     * 在庫操作コマンドの整合境界（[productIds]）が、 予約した全商品（[reservedItems]＝権威）をカバーしているか検証する。
     *
     * ドライバ（sweeper/webhook/reactor）は productIds を read model（order_items）から読んでコマンドに載せるが、
     * read model が未追従だと商品が欠ける。 欠けたまま進むと ProductsState にその商品が載らず、 在庫の確定（commit）/
     * 解放（release）が負値（onHandOf/reservedOf の 0 default − quantity）になり read model を破損させる。
     * 不一致は check で例外にして processor のリトライに乗せる（追従後の再実行で正常化。 永続欠損なら止まって気づける）。
     */
    fun requireReservedProductsCovered(
        orderId: String,
        productIds: Collection<String>,
    ) {
        val passed = productIds.toSet()
        reservedItems.forEach { item ->
            check(item.productId in passed) {
                "在庫操作の整合境界が不足（read model 未追従の可能性）: order=$orderId product=${item.productId}"
            }
        }
    }

    @EventSourcingHandler
    fun evolve(event: OrderStartedEvent) {
        status = OrderStatus.STARTED
        reservedItems.addAll(event.items.map { ReservedItem(it.productId, it.quantity) })
        totalAmount = event.items.sumOf { it.unitPrice.toLong() * it.quantity }
    }

    @EventSourcingHandler
    fun evolve(event: OrderPaymentPreparedEvent) {
        status = OrderStatus.PAYMENT_PENDING
        paymentIntentId = event.paymentIntentId
    }

    @EventSourcingHandler
    fun evolve(event: OrderPaidEvent) {
        status = OrderStatus.PAID
    }

    @EventSourcingHandler
    fun evolve(event: OrderShippedEvent) {
        status = OrderStatus.SHIPPED
    }

    @EventSourcingHandler
    fun evolve(event: OrderCompletedEvent) {
        status = OrderStatus.COMPLETED
    }

    @EventSourcingHandler
    fun evolve(event: OrderFailedEvent) {
        status = OrderStatus.FAILED
    }
}
