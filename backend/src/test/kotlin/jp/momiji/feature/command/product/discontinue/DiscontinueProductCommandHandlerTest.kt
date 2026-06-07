package jp.momiji.feature.command.product.discontinue

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.feature.command.product.discontinue.DiscontinueProductCommand
import jp.momiji.feature.command.product.discontinue.DiscontinueProductCommandResult
import org.junit.jupiter.api.Test

class DiscontinueProductCommandHandlerTest : MomijiIntegrationTestBase() {
    private val brandId = "01HXYZBRAND0000000000000PD1"

    private fun createdEvent(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = brandId,
            name = "テスト商品",
            description = "テスト説明",
            imageUrl = null,
            price = 1000,
        )

    @Test
    fun `正常系_商品の生産終了成功`() {
        val productId = "01HXYZPRODUCT000000000000D1"

        fixture
            .given()
            .events(createdEvent(productId))
            .`when`()
            .command(DiscontinueProductCommand(id = productId))
            .then()
            .resultMessagePayload(DiscontinueProductCommandResult.success())
            .events(
                ProductDiscontinuedEvent(id = productId),
            )
    }

    @Test
    fun `異常系_未作成のまま生産終了すると productNotFound`() {
        val productId = "01HXYZPRODUCT000000000000D2"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(DiscontinueProductCommand(id = productId))
            .then()
            .resultMessagePayload(DiscontinueProductCommandResult.productNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_すでに生産終了済みなら成功を返しイベントは出ない`() {
        val productId = "01HXYZPRODUCT000000000000D3"

        fixture
            .given()
            .events(
                createdEvent(productId),
                ProductDiscontinuedEvent(id = productId),
            ).`when`()
            .command(DiscontinueProductCommand(id = productId))
            .then()
            .resultMessagePayload(DiscontinueProductCommandResult.success())
            .noEvents()
    }
}
