package jp.momiji.feature.product.update

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.product.ProductDescription
import jp.momiji.domain.product.ProductImageUrl
import jp.momiji.domain.product.ProductName
import jp.momiji.domain.product.ProductPrice
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.product.ProductUpdatedEvent
import org.junit.jupiter.api.Test

class UpdateProductCommandHandlerTest : MomijiIntegrationTestBase() {
    private val brandId = "01HXYZBRAND0000000000000PU1"

    private fun command(productId: String) =
        UpdateProductCommand(
            id = productId,
            name = ProductName.create("新商品名").get()!!,
            description = ProductDescription.create("新説明").get()!!,
            imageUrl = ProductImageUrl.create("https://example.com/new.png").get(),
            price = ProductPrice.create(2000).get()!!,
        )

    @Test
    fun `正常系_商品更新成功`() {
        val productId = "01HXYZPRODUCT000000000000U1"

        fixture
            .given()
            .events(
                ProductCreatedEvent(
                    id = productId,
                    brandId = brandId,
                    name = "旧商品名",
                    description = "旧説明",
                    imageUrl = null,
                    price = 1000,
                ),
            ).`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(UpdateProductCommandResult.success())
            .events(
                ProductUpdatedEvent(
                    id = productId,
                    name = "新商品名",
                    description = "新説明",
                    imageUrl = "https://example.com/new.png",
                    price = 2000,
                ),
            )
    }

    @Test
    fun `異常系_未作成のまま更新すると productNotFound`() {
        val productId = "01HXYZPRODUCT000000000000U2"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(UpdateProductCommandResult.productNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_廃番済み商品の更新は productNotFound`() {
        val productId = "01HXYZPRODUCT000000000000U3"

        fixture
            .given()
            .events(
                ProductCreatedEvent(
                    id = productId,
                    brandId = brandId,
                    name = "旧商品名",
                    description = "旧説明",
                    imageUrl = null,
                    price = 1000,
                ),
                ProductDiscontinuedEvent(id = productId),
            ).`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(UpdateProductCommandResult.productNotFound())
            .noEvents()
    }
}
