package jp.momiji.feature.product.update

import jp.momiji.domain.BusinessError
import jp.momiji.domain.product.ProductDescription
import jp.momiji.domain.product.ProductImageUrl
import jp.momiji.domain.product.ProductName
import jp.momiji.domain.product.ProductPrice
import jp.momiji.feature.CommandResult
import kotlinx.coroutines.future.await
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.annotation.TargetEntityId

data class UpdateProductCommand(
    @TargetEntityId
    val id: String,
    val name: ProductName,
    val description: ProductDescription,
    val imageUrl: ProductImageUrl?,
    val price: ProductPrice,
)

object UpdateProductCommandResult {
    fun success() = CommandResult.success()

    fun productNotFound() = CommandResult.fail(BusinessError("商品が存在しませんでした"))
}

suspend fun CommandGateway.updateProduct(command: UpdateProductCommand): CommandResult = send(command, CommandResult::class.java).await()
