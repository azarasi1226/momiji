package jp.momiji.feature.command.user.shippingaddress.changedefault

import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.shippingaddress.UserShippingAddressState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class ChangeDefaultShippingAddressCommandHandler {
    @CommandHandler
    fun handle(
        command: ChangeDefaultShippingAddressCommand,
        @InjectEntity state: UserShippingAddressState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return ChangeDefaultShippingAddressCommandResult.userNotFound()
        }
        // 削除と違い「無い配送先をデフォルトにする」はゴール不能なので、 不在は冪等成功でなく error。
        val target =
            state.addresses[command.shippingAddressId]
                ?: return ChangeDefaultShippingAddressCommandResult.addressNotFound()

        // 冪等性: 既にデフォルトなら状態は変わらないのでイベントを出さず成功。
        if (target.isDefault) {
            return ChangeDefaultShippingAddressCommandResult.success()
        }

        eventAppender.append(
            DefaultShippingAddressChangedEvent(
                userId = command.userId,
                shippingAddressId = command.shippingAddressId,
            ),
        )
        return ChangeDefaultShippingAddressCommandResult.success()
    }
}
