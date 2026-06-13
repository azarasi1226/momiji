package jp.momiji.projection.payment

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.LOOKUP_EMAIL
import iss.jooq.generated.tables.references.PAYMENT_METHODS
import iss.jooq.generated.tables.references.USERS
import jp.momiji.event.payment.CardDeletedEvent
import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.payment.DefaultCardChangedEvent
import jp.momiji.event.payment.StripeCustomerRegisteredEvent
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
 * 保存カード ReadModel（payment_methods）と users.stripe_customer_id を更新する projection。
 *
 * 他の read model projector（[jp.momiji.projection.user.UserTableProjector] 等）と同様、 既定の
 * Event Processor に乗る（lookup と違い CommandHandler から同期的に読まれないため専用 processor は不要）。
 */
@Component
class PaymentMethodTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(event: StripeCustomerRegisteredEvent) {
        val updated =
            dsl
                .update(USERS)
                .set(USERS.STRIPE_CUSTOMER_ID, event.stripeCustomerId)
                .where(USERS.ID.eq(event.userId))
                .execute()
        // users 行を作るのは別 processor（jp.momiji.projection.user）で順序保証がないため、 0 行更新になりうる。
        // 0 行の正体は 2 つあり、 扱いが逆なので lookup（subscribing = 同期更新でラグ無しの真実）で判別する:
        //   一時（user projector がまだ遅れているだけ）→ throw して processor の無限リトライに乗せる（ms 後に治癒）。
        //   恒久（ユーザー削除済みで users 行は二度と現れない）→ warn で握る。
        if (updated == 0) {
            val userExists = dsl.fetchExists(dsl.selectFrom(LOOKUP_EMAIL).where(LOOKUP_EMAIL.USER_ID.eq(event.userId)))
            if (userExists) {
                throw IllegalStateException(
                    "users 行が未投影のため stripe_customer_id を反映できません（リトライで治癒する想定）: userId=${event.userId}",
                )
            }
            logger.warn { "stripe_customer_id の反映先ユーザーは削除済みのためスキップ: userId=${event.userId}" }
        }
    }

    @EventHandler
    fun on(
        event: CardRegisteredEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        dsl
            .insertInto(PAYMENT_METHODS)
            .set(PAYMENT_METHODS.ID, event.paymentMethodId)
            .set(PAYMENT_METHODS.USER_ID, event.userId)
            .set(PAYMENT_METHODS.BRAND, event.brand)
            .set(PAYMENT_METHODS.LAST4, event.last4)
            .set(PAYMENT_METHODS.EXP_MONTH, event.expMonth)
            .set(PAYMENT_METHODS.EXP_YEAR, event.expYear)
            // default の付与は DefaultCardChangedEvent（同コマンドで一括追記される）が担うため、 登録時点では常に false。
            .set(PAYMENT_METHODS.IS_DEFAULT, false)
            .set(PAYMENT_METHODS.CREATED_AT, at)
            .set(PAYMENT_METHODS.UPDATED_AT, at)
            // 冪等性: webhook 再送由来で同じ pm_ が来ても二重 insert しない（コマンド側でも冪等だが二重防御）。
            .onDuplicateKeyIgnore()
            .execute()
    }

    @EventHandler
    fun on(event: UserDeletedEvent) {
        // ユーザー削除でそのユーザーのカード行を一括削除（孤児行を残さない）。 カード無しユーザーの 0 行は正常。
        dsl
            .deleteFrom(PAYMENT_METHODS)
            .where(PAYMENT_METHODS.USER_ID.eq(event.id))
            .execute()
    }

    @EventHandler
    fun on(event: CardDeletedEvent) {
        val deleted =
            dsl
                .deleteFrom(PAYMENT_METHODS)
                .where(PAYMENT_METHODS.ID.eq(event.paymentMethodId))
                .execute()
        // 冪等性: 対象不在は例外でなく warn で握る（CLAUDE.md 規約）。
        if (deleted == 0) {
            logger.warn { "削除対象のカードが見つかりません: paymentMethodId=${event.paymentMethodId}" }
        }
    }

    @EventHandler
    fun on(
        event: DefaultCardChangedEvent,
        @Timestamp timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        // 同一ユーザーの既存 default を全て落としてから、
        dsl
            .update(PAYMENT_METHODS)
            .set(PAYMENT_METHODS.IS_DEFAULT, false)
            .set(PAYMENT_METHODS.UPDATED_AT, at)
            .where(PAYMENT_METHODS.USER_ID.eq(event.userId))
            .execute()
        // 対象 pm_ を default にする。
        val updated =
            dsl
                .update(PAYMENT_METHODS)
                .set(PAYMENT_METHODS.IS_DEFAULT, true)
                .set(PAYMENT_METHODS.UPDATED_AT, at)
                .where(PAYMENT_METHODS.ID.eq(event.paymentMethodId))
                .execute()
        // CardRegistered の insert が先に処理される前提（同一コマンド追記順 + 既定 sequencing policy の全件直列）が
        // 成り立つ限り 0 行にはならない。 黙って default 不在に陥らないよう痕跡を残す（規約: 対象不在は warn）。
        if (updated == 0) {
            logger.warn { "default 反映先のカード行がありません: paymentMethodId=${event.paymentMethodId}" }
        }
    }
}
