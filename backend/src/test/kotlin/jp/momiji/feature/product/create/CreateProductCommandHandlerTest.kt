package jp.momiji.feature.product.create

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.product.ProductDescription
import jp.momiji.domain.product.ProductImageUrl
import jp.momiji.domain.product.ProductName
import jp.momiji.domain.product.ProductPrice
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.feature.command.product.create.CreateProductCommand
import jp.momiji.feature.command.product.create.CreateProductCommandResult
import org.junit.jupiter.api.Test

class CreateProductCommandHandlerTest : MomijiIntegrationTestBase() {
    private val brandId = "01HXYZBRAND0000000000000PC1"

    private fun command(productId: String) =
        CreateProductCommand(
            id = productId,
            brandId = brandId,
            name = ProductName.create("テスト商品").get()!!,
            description = ProductDescription.create("テスト説明").get()!!,
            imageUrl = ProductImageUrl.create(null).get(),
            price = ProductPrice.create(1000).get()!!,
        )

    @Test
    fun `正常系_ブランドが存在すれば商品作成成功`() {
        val productId = "01HXYZPRODUCT000000000000C1"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = ""),
            ).`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(CreateProductCommandResult.success())
            .events(
                ProductCreatedEvent(
                    id = productId,
                    brandId = brandId,
                    name = "テスト商品",
                    description = "テスト説明",
                    imageUrl = null,
                    price = 1000,
                ),
            )
    }

    @Test
    fun `異常系_ブランドが存在しなければ brandNotFound`() {
        val productId = "01HXYZPRODUCT000000000000C2"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(CreateProductCommandResult.brandNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_ブランドがアーカイブ済みなら brandNotFound`() {
        val productId = "01HXYZPRODUCT000000000000C3"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = ""),
                BrandArchivedEvent(id = brandId),
            ).`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(CreateProductCommandResult.brandNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_同じIDで既に作成済みなら新規イベントを出さず成功`() {
        val productId = "01HXYZPRODUCT000000000000C4"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = ""),
                ProductCreatedEvent(
                    id = productId,
                    brandId = brandId,
                    name = "既存商品",
                    description = "既存説明",
                    imageUrl = null,
                    price = 500,
                ),
            ).`when`()
            .command(command(productId))
            .then()
            .resultMessagePayload(CreateProductCommandResult.success())
            .noEvents()
    }
}
