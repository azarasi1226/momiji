package jp.momiji.event.payment

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * カード（PaymentMethod, pm_）をユーザーに登録したイベント。 「カードが増えた」という事実だけを表す。
 *
 * 生カード番号は持たず、 表示・選択用の　brand / 下 4 桁 / 有効期限のみを載せる。
 *
 * default かどうかはこのイベントは関知しない —— default の付与・移動はすべて
 * [DefaultCardChangedEvent] が担う（初回登録時はコマンドが 2 イベントを一括追記する）。
 * 役割を分けることで、 default の変化経路が 1 イベント型に一本化される（配送先と同じ設計）。
 */
@Event(namespace = "momiji.payment", name = "CardRegisteredEvent")
data class CardRegisteredEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    @EventTag(key = MomijiEventTag.PAYMENT_METHOD_ID)
    val paymentMethodId: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
)
