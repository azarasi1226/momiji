package jp.momiji.feature.command.user.shippingaddress

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.eventQualifiedName
import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.event.user.ShippingAddressDeletedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.event.user.UserDeletedEvent
import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * 配送先のコマンド群が共有する **user 単位** の DCB State（[jp.momiji.feature.command.payment.UserPaymentState] と同型）。
 *
 * 整合境界は `user_id` 単一タグ。 1 ユーザーの「存在」「配送先の集合」「どれが default か」を 1 つの State で見る。
 * 不変条件「配送先が 1 件でもあれば必ず default が 1 件ある」をこの State 上のガードで守る。
 *
 * **lean に保つ**: 配送先の住所フィールド等は持たない（ガードに必要な id と isDefault だけ）。
 * そのため ShippingAddressUpdatedEvent はこの State に不要で、 criteria にも意図的に含めていない
 * （編集は id と default に影響しないため。 値の冪等チェックもしない設計）。
 */
@EventSourced(idType = String::class)
class UserShippingAddressState(
    var userExists: Boolean,
    // shippingAddressId -> 配送先。 存在確認・初回 default 判定・default 冪等判定に使う。
    val addresses: MutableMap<String, Entry>,
) {
    class Entry(
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
                    ShippingAddressRegisteredEvent::class.eventQualifiedName(),
                    ShippingAddressDeletedEvent::class.eventQualifiedName(),
                    DefaultShippingAddressChangedEvent::class.eventQualifiedName(),
                )
    }

    @EntityCreator
    constructor() : this(
        userExists = false,
        addresses = mutableMapOf(),
    )

    @EventSourcingHandler
    fun evolve(event: UserCreatedEvent) {
        userExists = true
    }

    @EventSourcingHandler
    fun evolve(event: UserDeletedEvent) {
        userExists = false
        addresses.clear()
    }

    @EventSourcingHandler
    fun evolve(event: ShippingAddressRegisteredEvent) {
        // default の付与は別イベント（DefaultShippingAddressChangedEvent）が担う。 登録時点では非 default。
        addresses[event.shippingAddressId] = Entry(isDefault = false)
    }

    @EventSourcingHandler
    fun evolve(event: ShippingAddressDeletedEvent) {
        addresses.remove(event.shippingAddressId)
    }

    @EventSourcingHandler
    fun evolve(event: DefaultShippingAddressChangedEvent) {
        addresses.forEach { (shippingAddressId, entry) ->
            entry.isDefault = shippingAddressId == event.shippingAddressId
        }
    }
}
