package jp.momiji.feature.command.order.preparepayment

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway

/**
 * 決済準備の「事実」を記録するコマンド（STARTED → PAYMENT_PENDING）。
 *
 * Stripe での PaymentIntent 作成（外部 IO）は gRPC service 側で行い、 確定した pi_/pm_ をこのコマンドで記録する
 * （CommandHandler は外部 IO をしない。 PrepareCardRegistration と同じ二相）。 STARTED のときだけ
 * [jp.momiji.event.order.OrderPaymentPreparedEvent] を冪等に発行する。
 */
data class PreparePaymentCommand(
    val orderId: String,
    val paymentMethodId: String,
    val paymentIntentId: String,
    // PaymentIntent を作った課金額（service が read model から算出）。 注文時点の権威合計と照合するために載せる。
    val amount: Long,
)

object PreparePaymentCommandResult {
    fun success() = CommandResult.success()

    fun orderNotFound() = CommandResult.fail(BusinessError("注文が見つかりません"))

    // 課金額が注文時点の権威合計と食い違う（read model 未追従の可能性）。 client_secret を返さず課金を止める。
    fun amountMismatch() = CommandResult.fail(BusinessError("注文金額が一致しません"))
}

suspend fun CommandGateway.preparePayment(command: PreparePaymentCommand): CommandResult = send(command, CommandResult::class.java).await()
