package jp.momiji.feature.command.order.recordpaid

import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 決済成功の「事実」を記録するコマンド（PAYMENT_PENDING → PAID）
 */
data class RecordOrderPaidCommand(
    val orderId: String,
)

/**
 * [refundRequired]=true: 注文が既に FAILED / 不在で、 もう履行できない → **返金が必要**。
 * false: PAID にできた（または既に PAID で冪等）→ 返金不要。
 */
class RecordOrderPaidResult(
    val refundRequired: Boolean,
) {
    companion object {
        fun paid() = RecordOrderPaidResult(refundRequired = false)

        fun needsRefund() = RecordOrderPaidResult(refundRequired = true)
    }
}

suspend fun CommandGateway.recordOrderPaid(command: RecordOrderPaidCommand): RecordOrderPaidResult =
    send(command, RecordOrderPaidResult::class.java).await()
