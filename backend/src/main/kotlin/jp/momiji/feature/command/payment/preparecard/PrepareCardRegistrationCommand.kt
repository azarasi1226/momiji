package jp.momiji.feature.command.payment.preparecard

import jp.momiji.domain.BusinessError
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * Stripe Customer をユーザーに紐付ける（記録する）コマンド。
 *
 * Customer の作成と SetupIntent 作成という外部 IO は GrpcService 側で行い、 確定した [stripeCustomerId] を
 * このコマンドで「事実」として記録する（CommandHandler は外部 IO をしない）。 初回のみ
 * [jp.momiji.event.payment.StripeCustomerRegisteredEvent] を冪等に発行する。
 */
data class PrepareCardRegistrationCommand(
    @TargetEntityId
    val userId: String,
    val stripeCustomerId: String,
)

object PrepareCardRegistrationCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))
}

suspend fun CommandGateway.prepareCardRegistration(command: PrepareCardRegistrationCommand): CommandResult =
    send(command, CommandResult::class.java).await()
