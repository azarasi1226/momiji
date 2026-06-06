package jp.momiji.feature.brand.archive

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.feature.command.brand.archive.ArchiveBrandCommand
import jp.momiji.feature.command.brand.archive.ArchiveBrandCommandResult
import org.junit.jupiter.api.Test

class ArchiveBrandCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_ブランドアーカイブ成功`() {
        val brandId = "01HXYZBRAND00000000000000A1"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = "テスト説明"),
            ).`when`()
            .command(
                ArchiveBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(ArchiveBrandCommandResult.success())
            .events(
                BrandArchivedEvent(id = brandId),
            )
    }

    @Test
    fun `異常系_未作成のままアーカイブするとbrandNotFound`() {
        val brandId = "01HXYZBRAND00000000000000A2"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                ArchiveBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(ArchiveBrandCommandResult.brandNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_すでにアーカイブ済みなら成功を返しイベントは出ない`() {
        val brandId = "01HXYZBRAND00000000000000A3"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = "テスト説明"),
                BrandArchivedEvent(id = brandId),
            ).`when`()
            .command(
                ArchiveBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(ArchiveBrandCommandResult.success())
            .noEvents()
    }

    @Test
    fun `正常系_紐づく商品が残っていてもアーカイブできる（孤児化しない）`() {
        val brandId = "01HXYZBRAND00000000000000A4"

        // 商品が紐づいていてもアーカイブは商品を消さない（ライフサイクル化でガード撤去）。
        // ProductCreatedEvent は brand_id タグを持たないため、 そもそも archive の State には入らない。
        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "テストブランド", description = "テスト説明"),
            ).`when`()
            .command(
                ArchiveBrandCommand(id = brandId),
            ).then()
            .resultMessagePayload(ArchiveBrandCommandResult.success())
            .events(
                BrandArchivedEvent(id = brandId),
            )
    }
}
