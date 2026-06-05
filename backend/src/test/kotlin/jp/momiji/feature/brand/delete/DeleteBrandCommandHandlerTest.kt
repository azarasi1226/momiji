package jp.momiji.feature.brand.delete

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandDeletedEvent
import org.junit.jupiter.api.Test

class DeleteBrandCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_ブランド削除成功`() {
        val brandId = "01HXYZBRAND00000000000000D1"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = "テスト説明"),
            ).`when`()
            .command(
                DeleteBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(DeleteBrandCommandResult.success())
            .events(
                BrandDeletedEvent(id = brandId),
            )
    }

    @Test
    fun `異常系_未作成のまま削除するとbrandNotFound`() {
        val brandId = "01HXYZBRAND00000000000000D2"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                DeleteBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(DeleteBrandCommandResult.brandNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_すでに削除済みなら成功を返しイベントは出ない`() {
        val brandId = "01HXYZBRAND00000000000000D3"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = "テスト説明"),
                BrandDeletedEvent(id = brandId),
            ).`when`()
            .command(
                DeleteBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(DeleteBrandCommandResult.success())
            .noEvents()
    }
}
