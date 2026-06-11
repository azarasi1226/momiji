package jp.momiji.feature.command.user.shippingaddress.update

import jp.momiji.event.user.ShippingAddressUpdatedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.shippingaddress.UserShippingAddressState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class UpdateShippingAddressCommandHandler {
    @CommandHandler
    fun handle(
        command: UpdateShippingAddressCommand,
        @InjectEntity state: UserShippingAddressState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return UpdateShippingAddressCommandResult.userNotFound()
        }
        // 削除と違い「存在しない配送先を編集する」はゴール不能なので、 不在は冪等成功でなく error。
        if (!state.addresses.containsKey(command.shippingAddressId)) {
            return UpdateShippingAddressCommandResult.addressNotFound()
        }

        // 値の冪等チェック（同値なら no-op）はしない: State を lean に保つ設計のため、 編集前の値を持っていない。
        // 二重送信で同値 Updated が 2 回出ても projection が冪等に上書きするだけで実害なし。
        eventAppender.append(
            ShippingAddressUpdatedEvent(
                userId = command.userId,
                shippingAddressId = command.shippingAddressId,
                name = command.name.value,
                phoneNumber = command.phoneNumber.value,
                postalCode = command.postalCode.value,
                prefecture = command.prefecture.value,
                city = command.city.value,
                streetAddress = command.streetAddress.value,
                building = command.building.value,
                deliveryNote = command.deliveryNote.value,
            ),
        )
        return UpdateShippingAddressCommandResult.success()
    }
}
