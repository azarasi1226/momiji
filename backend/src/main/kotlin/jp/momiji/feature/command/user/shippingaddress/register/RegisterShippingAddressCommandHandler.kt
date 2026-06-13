package jp.momiji.feature.command.user.shippingaddress.register

import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.user.shippingaddress.UserShippingAddressState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class RegisterShippingAddressCommandHandler {
    @CommandHandler
    fun handle(
        command: RegisterShippingAddressCommand,
        @InjectEntity state: UserShippingAddressState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.userExists) {
            return RegisterShippingAddressCommandResult.userNotFound()
        }
        // 冪等性: id は BFF が採番して渡す。 同じ id での再送（リトライ）は新規イベントを出さず成功。
        if (state.addresses.containsKey(command.id)) {
            return RegisterShippingAddressCommandResult.success()
        }
        if (state.addresses.size >= MAX_ADDRESS_COUNT) {
            return RegisterShippingAddressCommandResult.maxCountOver()
        }

        eventAppender.append(
            ShippingAddressRegisteredEvent(
                userId = command.userId,
                shippingAddressId = command.id,
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

        // makeDefault 指定 が指定されている、または default が 1 件も存在しない場合（つまり初回）
        // （初回登録。 万が一 default 不在の不変条件が壊れていてもここで自己修復される）。
        if (command.makeDefault || state.addresses.values.none { it.isDefault }) {
            eventAppender.append(
                DefaultShippingAddressChangedEvent(
                    userId = command.userId,
                    shippingAddressId = command.id,
                ),
            )
        }
        return RegisterShippingAddressCommandResult.success()
    }

    companion object {
        // 1 ユーザーが登録できる配送先の上限（カゴの商品種類数上限と同じ「際限なく増やさない」ガード）
        const val MAX_ADDRESS_COUNT = 10
    }
}
