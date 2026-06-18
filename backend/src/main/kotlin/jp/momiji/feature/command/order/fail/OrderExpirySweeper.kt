package jp.momiji.feature.command.order.fail

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.ORDERS
import jp.momiji.domain.order.OrderFailureReason
import jp.momiji.domain.order.OrderStatus
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderProductIdsReader
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.jooq.DSLContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

/**
 * 注文締め切りスイーパー
 * 確定していない（STARTED / PAYMENT_PENDING）まま期限を過ぎた注文を定期的に拾い、
 * [FailOrderCommand]（reason=EXPIRED）で FAILED にする。
 *
 * **2 系統**で掃く（3DS レース対策)
 * - STARTED: オーダーがスタートしたが決済に着手していない。 [STARTED_TTL] を `created_at` 基準
 * - PAYMENT_PENDING: 決済着手したが未完了（3DS 放置）。 [PAYMENT_PENDING_TTL] を `updated_at` 基準
 *
 * 時計が [FailOrderCommand] を起動する入口（time-driven inbound adapter）なので、 fail ユースケースと同居する。
 */
@Component
class OrderExpirySweeper(
    private val dsl: DSLContext,
    private val commandGateway: CommandGateway,
    private val orderProductIdsReader: OrderProductIdsReader,
) {
    @Scheduled(fixedDelayString = SWEEP_INTERVAL)
    fun sweep() {
        val expiredOrderIds = findExpiredOrderIds()
        if (expiredOrderIds.isEmpty()) return

        for (orderId in expiredOrderIds) {
            try {
                commandGateway
                    .send(
                        FailOrderCommand(
                            orderId = orderId,
                            productIds = orderProductIdsReader.read(orderId),
                            reason = OrderFailureReason.EXPIRED,
                        ),
                        CommandResult::class.java,
                    ).get()
            } catch (e: Exception) {
                // 1 件の失敗で sweep 全体を止めない。 次回 sweep で再試行（冪等なので安全）
                logger.warn(e) { "注文の失効に失敗（次回 sweep で再試行）: orderId=$orderId" }
            }
        }
        logger.info { "注文タイムアウトの失効処理: ${expiredOrderIds.size} 件" }
    }

    /**
     * 締め切りを過ぎた未確定ステータスの orderId 検索
     *
     * - STARTED で created_at が古い（決済未着手のまま放置）
     * - PAYMENT_PENDING で updated_at が古い（決済着手後 = 3DS等で放置）
     */
    private fun findExpiredOrderIds(): List<String> {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        return dsl
            .select(ORDERS.ID)
            .from(ORDERS)
            .where(
                ORDERS.STATUS
                    .eq(OrderStatus.STARTED.name)
                    .and(ORDERS.CREATED_AT.lt(now.minus(STARTED_TTL)))
                    .or(
                        ORDERS.STATUS
                            .eq(OrderStatus.PAYMENT_PENDING.name)
                            .and(ORDERS.UPDATED_AT.lt(now.minus(PAYMENT_PENDING_TTL))),
                    ),
            ).fetch(ORDERS.ID)
            .filterNotNull()
    }

    companion object {
        // 注文がスタートしてから、決済がスタートするまでの締め切り時間
        // 基本的に人間が介入しないオートな操作のため締め切り時間は短く
        private val STARTED_TTL: Duration = Duration.ofMinutes(5)

        // 決済スタートしたあとに実際に決済が終了するまでの締め切り時間
        // 3d secure などの人間が操作するため締め切り時間は長くする必要がある。
        // 一般的な 3d secure の締め切り時間は約10分なので、その２倍を設定
        private val PAYMENT_PENDING_TTL: Duration = Duration.ofMinutes(20)

        // sweep 間隔
        private const val SWEEP_INTERVAL = "PT1M"
    }
}
