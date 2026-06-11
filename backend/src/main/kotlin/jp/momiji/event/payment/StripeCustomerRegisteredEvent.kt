package jp.momiji.event.payment

import jp.momiji.event.MomijiEventTag
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * Stripe Customer（cus_）をユーザーに紐付けたイベント。
 *
 * Customer は **lazy 作成**（初回カード登録の準備時にだけ作る）なので、 このイベントは
 * ユーザーごとに高々 1 回だけ発行される。 projection が [users.stripe_customer_id] を埋める。
 */
@Event(namespace = "momiji.payment", name = "StripeCustomerRegisteredEvent")
data class StripeCustomerRegisteredEvent(
    @EventTag(key = MomijiEventTag.USER_ID)
    val userId: String,
    val stripeCustomerId: String,
)
