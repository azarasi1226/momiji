package jp.momiji.feature.command.order.expire

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.ORDERS
import iss.jooq.generated.tables.references.ORDER_ITEMS
import jp.momiji.domain.order.OrderStatus
import jp.momiji.feature.command.CommandResult
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.jooq.DSLContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

/**
 * 予約タイムアウトのスイーパー。 STARTED のまま [RESERVATION_TTL] を過ぎた注文（＝決済が完了しなかった放置）を
 * 定期的に拾い、 [ExpireOrderCommand] で在庫予約を解放して FAILED にする。
 *
 * - **read model（orders）を引くだけ**でイベント再生はしないので、 ADR 0013 の「reactor を replay させない」問題と無縁。
 * - 失効はコマンド側で「STARTED か」を再ガードするので、 sweep のラグで二重に撃っても冪等。
 * - 失敗した注文は次回 sweep で再試行される（1 件の失敗が他を止めない）。
 *
 * 注意（支払いフェーズ未実装の現状）: STARTED → PAID の遷移がまだ無いため、 全 STARTED 注文が TTL 後に失効する。
 * 決済を実装すると PAID が除外される。
 */
@Component
class OrderExpirySweeper(
    private val dsl: DSLContext,
    private val commandGateway: CommandGateway,
) {
    @Scheduled(fixedDelayString = SWEEP_INTERVAL)
    fun sweep() {
        val expiredOrderIds = findExpiredOrderIds()
        if (expiredOrderIds.isEmpty()) return

        for (orderId in expiredOrderIds) {
            try {
                commandGateway
                    .send(
                        ExpireOrderCommand(orderId = orderId, productIds = findOrderProductIds(orderId)),
                        CommandResult::class.java,
                    ).get()
            } catch (e: Exception) {
                // 1 件の失敗で sweep 全体を止めない。 次回 sweep で再試行（冪等なので安全）。
                logger.warn(e) { "注文の失効に失敗（次回 sweep で再試行）: orderId=$orderId" }
            }
        }
        logger.info { "予約タイムアウトの失効処理: ${expiredOrderIds.size} 件" }
    }

    /** STARTED のまま TTL を過ぎた注文の id を引く。 */
    private fun findExpiredOrderIds(): List<String> {
        val deadline = LocalDateTime.now(ZoneOffset.UTC).minus(RESERVATION_TTL)
        return dsl
            .select(ORDERS.ID)
            .from(ORDERS)
            .where(ORDERS.STATUS.eq(OrderStatus.STARTED.name))
            .and(ORDERS.CREATED_AT.lt(deadline))
            .fetch(ORDERS.ID)
            .filterNotNull()
    }

    /** 注文の明細から product_id 群を引く（ExpireOrder の整合境界を組むため）。 */
    private fun findOrderProductIds(orderId: String): List<String> =
        dsl
            .select(ORDER_ITEMS.PRODUCT_ID)
            .from(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetch(ORDER_ITEMS.PRODUCT_ID)
            .filterNotNull()

    companion object {
        // 予約の保持時間。 決済（3DS 含む）に十分な長さにする。
        private val RESERVATION_TTL: Duration = Duration.ofMinutes(30)

        // sweep 間隔（ISO-8601 Duration）。
        private const val SWEEP_INTERVAL = "PT1M"
    }
}
