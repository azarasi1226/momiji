package jp.momiji.feature.product.create

import jp.momiji.domain.BusinessError
import jp.momiji.domain.product.ProductDescription
import jp.momiji.domain.product.ProductImageUrl
import jp.momiji.domain.product.ProductName
import jp.momiji.domain.product.ProductPrice
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

/**
 * 商品作成コマンド。
 *
 * **2 つの id をターゲットにする**: 自分自身の [id] と、 紐づけ先ブランドの [brandId]。
 * これにより CommandHandler の DCB State が product_id と brand_id の両方のイベントストリームを
 * ロードでき、「 商品が既に作成済みか（冪等性）」と「 ブランドが存在するか 」を 1 回の整合境界で判定できる。
 */
data class CreateProductCommand(
    val id: String,
    val brandId: String,
    val name: ProductName,
    val description: ProductDescription,
    val imageUrl: ProductImageUrl?,
    val price: ProductPrice,
) {
    data class TargetId(
        val productId: String,
        val brandId: String,
    )

    @get:TargetEntityId
    private val targetId: TargetId
        get() = TargetId(productId = id, brandId = brandId)
}

object CreateProductCommandResult {
    fun success() = CommandResult.success()

    fun brandNotFound() = CommandResult.fail(BusinessError("ブランドが存在しません"))
}

suspend fun CommandGateway.createProduct(command: CreateProductCommand): CommandResult = send(command, CommandResult::class.java).await()
