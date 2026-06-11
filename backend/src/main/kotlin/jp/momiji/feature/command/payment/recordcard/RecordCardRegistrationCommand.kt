package jp.momiji.feature.command.payment.recordcard

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * 確定したカード（pm_）を記録するコマンド。
 *
 * 入口は Stripe の `setup_intent.succeeded` webhook。 webhook ハンドラが pm_ の表示情報（brand / 下 4 桁 /
 * 有効期限）を Stripe から取得して詰める。 そのユーザーの初回カードなら CommandHandler 側で default にする。
 */
data class RecordCardRegistrationCommand(
    @TargetEntityId
    val userId: String,
    val paymentMethodId: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
)

object RecordCardRegistrationCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))
}

suspend fun CommandGateway.recordCardRegistration(command: RecordCardRegistrationCommand): CommandResult =
    send(command, CommandResult::class.java).await()
