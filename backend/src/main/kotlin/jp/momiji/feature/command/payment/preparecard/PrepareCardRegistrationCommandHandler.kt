package jp.momiji.feature.command.payment.preparecard

import jp.momiji.event.payment.StripeCustomerRegisteredEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.payment.UserPaymentState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 初回 prepare（cus_ 未記録）のときに [StripeCustomerRegisteredEvent] を発行する CommandHandler。
 *
 * 2 回目以降は GrpcService 層（StripeCustomerReader）で完結し、 通常ここには到達しない。
 * ただし projection 反映前の並走や、 記録失敗後のリトライでは到達しうるため、 記録済みなら no-op の冪等ガードを持つ。
 */
@Component
class PrepareCardRegistrationCommandHandler {
    @CommandHandler
    fun handle(
        command: PrepareCardRegistrationCommand,
        @InjectEntity state: UserPaymentState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return PrepareCardRegistrationCommandResult.userNotFound()
        }
        // 冪等性: 既に Customer を記録済みなら新規イベントを出さず成功（並走した初回準備の二重記録を防ぐ）。
        if (state.stripeCustomerId != null) {
            return PrepareCardRegistrationCommandResult.success()
        }

        eventAppender.append(
            StripeCustomerRegisteredEvent(
                userId = command.userId,
                stripeCustomerId = command.stripeCustomerId,
            ),
        )
        return PrepareCardRegistrationCommandResult.success()
    }
}
