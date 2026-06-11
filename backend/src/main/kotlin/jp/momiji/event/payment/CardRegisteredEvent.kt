package jp.momiji.event.payment

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * カード（PaymentMethod, pm_）をユーザーに登録したイベント。
 *
 * 生カード番号は持たず、 表示・選択用の　brand / 下 4 桁 / 有効期限のみを載せる。 [default] はそのユーザーの初回カードのとき true。
 *
 * NOTE: フィールド名を `isDefault` にすると Jackson が `is` を剥がして `default` キーで永続化し、
 * 読み戻し時に `isDefault` を探して null になる（Kotlin boolean の `is` プレフィックス問題）。 そのため `default` で固定する。
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
    val default: Boolean,
)
