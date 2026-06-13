package jp.momiji.feature.command.user.shippingaddress.update

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
 * 配送先を編集するコマンド。 整合境界は user（State が user 単位）。
 * default の変更は別ユースケース（changedefault）が担う。
 */
data class UpdateShippingAddressCommand(
    @TargetEntityId
    val userId: String,
    val shippingAddressId: String,
    val name: Name,
    val phoneNumber: PhoneNumber,
    val postalCode: PostalCode,
    val prefecture: Prefecture,
    val city: City,
    val streetAddress: StreetAddress,
    val building: Building,
    val deliveryNote: DeliveryNote,
)

object UpdateShippingAddressCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))

    fun addressNotFound() = CommandResult.fail(BusinessError("配送先が存在しません"))
}

suspend fun CommandGateway.updateShippingAddress(command: UpdateShippingAddressCommand): CommandResult =
    send(command, CommandResult::class.java).await()
