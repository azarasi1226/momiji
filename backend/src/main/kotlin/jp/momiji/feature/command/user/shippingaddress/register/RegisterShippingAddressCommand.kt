package jp.momiji.feature.command.user.shippingaddress.register

import jp.momiji.domain.BusinessError
import jp.momiji.domain.user.Building
import jp.momiji.domain.user.City
import jp.momiji.domain.user.DeliveryNote
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.domain.user.Prefecture
import jp.momiji.domain.user.StreetAddress
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * 配送先を登録するコマンド。 整合境界は user（State が user 単位）。
 *
 * [id] は BFF が採番して渡す（冪等キー）。 [makeDefault] が true なら登録と同時にデフォルトへ昇格させる。
 */
data class RegisterShippingAddressCommand(
    @TargetEntityId
    val userId: String,
    val id: String,
    val name: Name,
    val phoneNumber: PhoneNumber,
    val postalCode: PostalCode,
    val prefecture: Prefecture,
    val city: City,
    val streetAddress: StreetAddress,
    val building: Building,
    val deliveryNote: DeliveryNote,
    val makeDefault: Boolean,
)

object RegisterShippingAddressCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))

    fun maxCountOver() = CommandResult.fail(BusinessError("登録できる配送先の上限に達しています"))
}

suspend fun CommandGateway.registerShippingAddress(command: RegisterShippingAddressCommand): CommandResult =
    send(command, CommandResult::class.java).await()
