package jp.momiji.feature.command.payment

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.payment.DefaultCardChangedEvent
import jp.momiji.event.payment.StripeCustomerRegisteredEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * カード登録・削除・デフォルト変更コマンドが共有する **user 単位** の DCB State。
 *
 * 整合境界は `user_id` 単一タグ。 1 ユーザーの「存在」「lazy Customer の有無」「保有カード集合」「default」を
 * 1 つの State で見て、 4 つの CommandHandler（prepare / record / delete / changedefault）が
 * `@InjectEntity` で共有する。
 *
 * **削除も考慮する**（CLAUDE.md 規約）: UserDeleted で無効化、 CardDeleted で map から除去する。
 */
@EventSourced(idType = String::class)
class UserPaymentState(
    var userExists: Boolean,
    // lazy Customer。 未作成なら null。 prepare の冪等判定（二重に StripeCustomerRegistered を出さない）に使う。
    var stripeCustomerId: String?,
    // pm_ -> Card。 カードの存在確認・初回 default 判定・default 冪等判定に使う。
    val cards: MutableMap<String, Card>,
) {
    class Card(
        var isDefault: Boolean,
    )

    companion object {
        @JvmStatic
        @EventCriteriaBuilder
        private fun resolveCriteria(userId: String): EventCriteria =
            EventCriteria
                .havingTags(Tag.of(MomijiEventTag.USER_ID, userId))
                .andBeingOneOfTypes(
                    UserCreatedEvent::class.eventQualifiedName(),
                    UserDeletedEvent::class.eventQualifiedName(),
                    StripeCustomerRegisteredEvent::class.eventQualifiedName(),
                    CardRegisteredEvent::class.eventQualifiedName(),
                    CardDeletedEvent::class.eventQualifiedName(),
                    DefaultCardChangedEvent::class.eventQualifiedName(),
                )
    }

    @EntityCreator
    constructor() : this(
        userExists = false,
        stripeCustomerId = null,
        cards = mutableMapOf(),
    )

    @EventSourcingHandler
    fun evolve(event: UserCreatedEvent) {
        userExists = true
    }

    @EventSourcingHandler
    fun evolve(event: UserDeletedEvent) {
        userExists = false
        stripeCustomerId = null
        cards.clear()
    }

    @EventSourcingHandler
    fun evolve(event: StripeCustomerRegisteredEvent) {
        stripeCustomerId = event.stripeCustomerId
    }

    @EventSourcingHandler
    fun evolve(event: CardRegisteredEvent) {
        // default の付与は別イベント（DefaultCardChangedEvent）が担う。 登録時点では非 default。
        cards[event.paymentMethodId] = Card(isDefault = false)
    }

    @EventSourcingHandler
    fun evolve(event: CardDeletedEvent) {
        cards.remove(event.paymentMethodId)
    }

    @EventSourcingHandler
    fun evolve(event: DefaultCardChangedEvent) {
        cards.forEach { (paymentMethodId, card) ->
            card.isDefault = paymentMethodId == event.paymentMethodId
        }
    }
}
