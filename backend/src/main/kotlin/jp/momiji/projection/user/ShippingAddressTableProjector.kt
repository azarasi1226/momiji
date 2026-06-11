package jp.momiji.projection.user

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.SHIPPING_ADDRESSES
import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent
import jp.momiji.event.user.UserDeletedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

/**
 * 配送先 ReadModel（shipping_addresses）を更新する projection。
 *
 * [jp.momiji.projection.payment.PaymentMethodTableProjector] と同型（既定の Event Processor に乗る）。
 * payment にあった「users 行未投影レース」はここには無い（自テーブルへの書き込みだけで完結し、
 * 同一 processor 内でイベント順序が保証される）。
 */
@Component
class ShippingAddressTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: ShippingAddressRegisteredEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        dsl
            .insertInto(SHIPPING_ADDRESSES)
            .set(SHIPPING_ADDRESSES.ID, event.shippingAddressId)
            .set(SHIPPING_ADDRESSES.USER_ID, event.userId)
            .set(SHIPPING_ADDRESSES.NAME, event.name)
            .set(SHIPPING_ADDRESSES.PHONE_NUMBER, event.phoneNumber)
            .set(SHIPPING_ADDRESSES.POSTAL_CODE, event.postalCode)
            .set(SHIPPING_ADDRESSES.PREFECTURE, event.prefecture)
            .set(SHIPPING_ADDRESSES.CITY, event.city)
            .set(SHIPPING_ADDRESSES.STREET_ADDRESS, event.streetAddress)
            .set(SHIPPING_ADDRESSES.BUILDING, event.building)
            .set(SHIPPING_ADDRESSES.DELIVERY_NOTE, event.deliveryNote)
            // default の付与は DefaultShippingAddressChangedEvent（同コマンドで一括追記される）が担うため、 登録時点では常に 0。
            .set(SHIPPING_ADDRESSES.IS_DEFAULT, false.toByteFlag())
            .set(SHIPPING_ADDRESSES.CREATED_AT, at)
            .set(SHIPPING_ADDRESSES.UPDATED_AT, at)
            // 冪等性: イベント再処理で同じ id が来ても二重 insert しない（コマンド側でも冪等だが二重防御）。
            .onDuplicateKeyIgnore()
            .execute()
    }

    @EventHandler
    fun on(
        event: DefaultShippingAddressChangedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        // 同一ユーザーの既存 default を全て落としてから、
        dsl
            .update(SHIPPING_ADDRESSES)
            .set(SHIPPING_ADDRESSES.IS_DEFAULT, false.toByteFlag())
            .set(SHIPPING_ADDRESSES.UPDATED_AT, at)
            .where(SHIPPING_ADDRESSES.USER_ID.eq(event.userId))
            .execute()
        // 対象の配送先を default にする。
        val updated =
            dsl
                .update(SHIPPING_ADDRESSES)
                .set(SHIPPING_ADDRESSES.IS_DEFAULT, true.toByteFlag())
                .set(SHIPPING_ADDRESSES.UPDATED_AT, at)
                .where(SHIPPING_ADDRESSES.ID.eq(event.shippingAddressId))
                .execute()
        // 上記前提が成り立つ限り 0 行にはならないはず。 黙って default 不在に陥らないよう痕跡を残す（規約: 対象不在は warn）。
        if (updated == 0) {
            logger.warn { "default 反映先の配送先行がありません: shippingAddressId=${event.shippingAddressId}" }
        }
    }

    @EventHandler
    fun on(event: UserDeletedEvent) {
        // ユーザー削除でそのユーザーの配送先を一括削除（孤児行を残さない）。 配送先なしユーザーの 0 行は正常。
        dsl
            .deleteFrom(SHIPPING_ADDRESSES)
            .where(SHIPPING_ADDRESSES.USER_ID.eq(event.id))
            .execute()
    }
}

// is_default は MySQL tinyint(1) = jOOQ では Byte。 Boolean を 0/1 に橋渡しする。
private fun Boolean.toByteFlag(): Byte = if (this) 1 else 0
