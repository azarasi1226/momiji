package jp.momiji.feature.command.user.shippingaddress.delete

import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.event.user.ShippingAddressDeletedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.shippingaddress.UserShippingAddressState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class DeleteShippingAddressCommandHandler {
    @CommandHandler
    fun handle(
        command: DeleteShippingAddressCommand,
        @InjectEntity state: UserShippingAddressState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return DeleteShippingAddressCommandResult.userNotFound()
        }
        // 冪等性: 既に削除済み（配送先が無い）なら、 望む終端状態（その配送先が無い状態）は達成済み。
        // エラーにせず no-op で成功を返す（二重クリックや再送でユーザーにエラーを見せない）。
        val target =
            state.addresses[command.shippingAddressId]
                ?: return DeleteShippingAddressCommandResult.success()

        eventAppender.append(
            ShippingAddressDeletedEvent(
                userId = command.userId,
                shippingAddressId = command.shippingAddressId,
            ),
        )

        // 不変条件「配送先が 1 件でもあれば必ず default が 1 件ある」を守る:
        // default を削除して他の配送先が残るなら、 最古の残り（addresses は再生順 = 登録順の LinkedHashMap）を昇格させる。
        if (target.isDefault) {
            val successor = state.addresses.keys.firstOrNull { it != command.shippingAddressId }
            if (successor != null) {
                eventAppender.append(
                    DefaultShippingAddressChangedEvent(
                        userId = command.userId,
                        shippingAddressId = successor,
                    ),
                )
            }
        }
        return DeleteShippingAddressCommandResult.success()
    }
}
