package jp.momiji.feature.command.stock.adjust

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.stock.AdjustStockQuantity
import jp.momiji.domain.stock.StockAdjustment
import jp.momiji.domain.stock.StockAdjustmentReason
import jp.momiji.domain.stock.StockQuantity
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.event.stock.StockReceivedEvent
import org.junit.jupiter.api.Test

class AdjustStockCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。
    private fun productCreated(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = "01HXYZBRAND0000000000000AJ1",
            name = "テスト商品",
            description = "説明",
            imageUrl = null,
            price = 1000,
        )

    private fun command(
        productId: String,
        quantity: Int,
        reason: StockAdjustmentReason,
    ) = AdjustStockCommand(
        productId = productId,
        adjustment = StockAdjustment.create(AdjustStockQuantity.create(quantity).get()!!, reason).get()!!,
    )

    @Test
    fun `正常系_増加方向の調整`() {
        val productId = "01HXYZPRODUCT0000000000AJ01"

        fixture
            .given()
            .events(
                productCreated(productId),
                StockReceivedEvent(productId = productId, receivedQuantity = 10, onHandQuantity = 10),
            ).`when`()
            .command(command(productId, 5, StockAdjustmentReason.STOCKTAKING))
            .then()
            .resultMessagePayload(AdjustStockCommandResult.success())
            .events(
                StockAdjustedEvent(
                    productId = productId,
                    adjustmentQuantity = 5,
                    reason = "STOCKTAKING",
                    onHandQuantity = 15,
                ),
            )
    }

    @Test
    fun `正常系_減少方向の調整`() {
        val productId = "01HXYZPRODUCT0000000000AJ02"

        fixture
            .given()
            .events(
                productCreated(productId),
                StockReceivedEvent(productId = productId, receivedQuantity = 10, onHandQuantity = 10),
            ).`when`()
            .command(command(productId, -4, StockAdjustmentReason.DAMAGED))
            .then()
            .resultMessagePayload(AdjustStockCommandResult.success())
            .events(
                StockAdjustedEvent(
                    productId = productId,
                    adjustmentQuantity = -4,
                    reason = "DAMAGED",
                    onHandQuantity = 6,
                ),
            )
    }

    @Test
    fun `異常系_商品が存在しなければ productNotFound`() {
        val productId = "01HXYZPRODUCT0000000000AJ03"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(productId, -1, StockAdjustmentReason.CORRECTION))
            .then()
            .resultMessagePayload(AdjustStockCommandResult.productNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_在庫が不足する減少なら stockShortage`() {
        val productId = "01HXYZPRODUCT0000000000AJ04"

        fixture
            .given()
            .events(
                productCreated(productId),
                StockReceivedEvent(productId = productId, receivedQuantity = 3, onHandQuantity = 3),
            ).`when`()
            .command(command(productId, -5, StockAdjustmentReason.LOST))
            .then()
            .resultMessagePayload(AdjustStockCommandResult.stockShortage())
            .noEvents()
    }

    @Test
    fun `異常系_調整で上限を超えるなら quantityOverflow`() {
        val productId = "01HXYZPRODUCT0000000000AJ05"

        fixture
            .given()
            .events(
                productCreated(productId),
                StockReceivedEvent(
                    productId = productId,
                    receivedQuantity = StockQuantity.MAX,
                    onHandQuantity = StockQuantity.MAX,
                ),
            ).`when`()
            .command(command(productId, 1, StockAdjustmentReason.STOCKTAKING))
            .then()
            .resultMessagePayload(AdjustStockCommandResult.quantityOverflow())
            .noEvents()
    }
}
