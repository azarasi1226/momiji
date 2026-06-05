package jp.momiji.feature.brand.create

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.brand.BrandDescription
import jp.momiji.domain.brand.BrandName
import jp.momiji.event.brand.BrandCreatedEvent
import org.junit.jupiter.api.Test

class CreateBrandCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_ブランド作成成功`() {
        val brandId = "01HXYZBRAND00000000000000C1"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                CreateBrandCommand(
                    id = brandId,
                    name = BrandName.create("テストブランド").get()!!,
                    description = BrandDescription.create("テスト説明").get()!!,
                ),
            ).then()
            .resultMessagePayload(CreateBrandCommandResult.success())
            .events(
                BrandCreatedEvent(
                    id = brandId,
                    name = "テストブランド",
                    description = "テスト説明",
                ),
            )
    }

    @Test
    fun `冪等性_同じIDで既に作成済みなら新規イベントを出さず成功`() {
        val brandId = "01HXYZBRAND00000000000000C2"

        fixture
            .given()
            .events(
                BrandCreatedEvent(
                    id = brandId,
                    name = "既存ブランド",
                    description = "",
                ),
            ).`when`()
            .command(
                CreateBrandCommand(
                    id = brandId,
                    name = BrandName.create("テストブランド").get()!!,
                    description = BrandDescription.create("テスト説明").get()!!,
                ),
            ).then()
            .resultMessagePayload(CreateBrandCommandResult.success())
            .noEvents()
    }
}
