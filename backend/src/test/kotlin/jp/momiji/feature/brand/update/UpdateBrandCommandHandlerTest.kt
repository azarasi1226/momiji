package jp.momiji.feature.brand.update

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.brand.BrandDescription
import jp.momiji.domain.brand.BrandName
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandDeletedEvent
import jp.momiji.event.brand.BrandUpdatedEvent
import org.junit.jupiter.api.Test

class UpdateBrandCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_ブランド更新成功`() {
        val brandId = "01HXYZBRAND00000000000000U1"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "旧ブランド名", description = "旧説明"),
            ).`when`()
            .command(
                UpdateBrandCommand(
                    id = brandId,
                    name = BrandName.create("新ブランド名").get()!!,
                    description = BrandDescription.create("新説明").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateBrandCommandResult.success())
            .events(
                BrandUpdatedEvent(
                    id = brandId,
                    name = "新ブランド名",
                    description = "新説明",
                ),
            )
    }

    @Test
    fun `異常系_未作成のまま更新するとbrandNotFound`() {
        val brandId = "01HXYZBRAND00000000000000U2"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                UpdateBrandCommand(
                    id = brandId,
                    name = BrandName.create("新ブランド名").get()!!,
                    description = BrandDescription.create("新説明").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateBrandCommandResult.brandNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_削除済みブランドの更新はbrandNotFound`() {
        val brandId = "01HXYZBRAND00000000000000U3"

        fixture
            .given()
            .events(
                BrandCreatedEvent(id = brandId, name = "旧ブランド名", description = "旧説明"),
                BrandDeletedEvent(id = brandId),
            ).`when`()
            .command(
                UpdateBrandCommand(
                    id = brandId,
                    name = BrandName.create("新ブランド名").get()!!,
                    description = BrandDescription.create("新説明").get()!!,
                ),
            ).then()
            .resultMessagePayload(UpdateBrandCommandResult.brandNotFound())
            .noEvents()
    }
}
