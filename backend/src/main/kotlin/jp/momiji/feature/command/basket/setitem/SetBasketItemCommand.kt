package jp.momiji.feature.command.basket.setitem

import jp.momiji.domain.BusinessError
import jp.momiji.domain.basket.BasketItemQuantity
import jp.momiji.feature.command.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * 買い物かごに商品をセット（追加 or 個数変更）するコマンド。
 *
 * カゴ（= user）と商品の **2 つの id をターゲット**にする。 これで CommandHandler の DCB State が
 * user_id と product_id の両ストリームをロードでき、「ユーザーが存在するか」「商品が ACTIVE か」
 * 「カゴの商品種類数が上限内か」を 1 整合境界で判定できる。
 */
data class SetBasketItemCommand(
    val userId: String,
    val productId: String,
    val itemQuantity: BasketItemQuantity,
) {
    data class TargetId(
        val userId: String,
        val productId: String,
    )

    @get:TargetEntityId
    private val targetId: TargetId
        get() = TargetId(userId = userId, productId = productId)
}

object SetBasketItemCommandResult {
    fun success() = CommandResult.success()

    fun userNotFound() = CommandResult.fail(BusinessError("ユーザーが存在しません"))

    fun productNotFound() = CommandResult.fail(BusinessError("商品が存在しません"))

    fun productMaxKindOver() = CommandResult.fail(BusinessError("カゴに入れられる商品種類数の上限に達しています"))
}

suspend fun CommandGateway.setBasketItem(command: SetBasketItemCommand): CommandResult = send(command, CommandResult::class.java).await()
