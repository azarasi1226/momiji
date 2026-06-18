package jp.momiji.feature.command.stock.receive

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.stock.ReceiveStockQuantity
import jp.momiji.domain.stock.StockQuantity
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationCommittedEvent
import org.junit.jupiter.api.Test

class ReceiveStockCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。
    private fun productCreated(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = "01HXYZBRAND0000000000000RC1",
            name = "テスト商品",
            description = "説明",
            imageUrl = null,
            price = 1000,
        )

    private fun command(
        productId: String,
        quantity: Int,
    ) = ReceiveStockCommand(
        productId = productId,
        receivedQuantity = ReceiveStockQuantity.create(quantity).get()!!,
    )

    @Test
    fun `正常系_商品が存在すれば入庫しonHandが増える`() {
        val productId = "01HXYZPRODUCT0000000000RC01"

        fixture
            .given()
            .events(productCreated(productId))
            .`when`()
            .command(command(productId, 10))
            .then()
            .resultMessagePayload(ReceiveStockCommandResult.success())
            .events(
                StockReceivedEvent(productId = productId, receivedQuantity = 10, onHandQuantity = 10),
            )
    }

    @Test
    fun `正常系_既存在庫に加算される`() {
        val productId = "01HXYZPRODUCT0000000000RC02"

        fixture
            .given()
            .events(
                productCreated(productId),
                StockReceivedEvent(productId = productId, receivedQuantity = 10, onHandQuantity = 10),
            ).`when`()
            .command(command(productId, 5))
            .then()
            .resultMessagePayload(ReceiveStockCommandResult.success())
            .events(
                StockReceivedEvent(productId = productId, receivedQuantity = 5, onHandQuantity = 15),
            )
    }

    @Test
    fun `正常系_出荷確定で減った後の onHand に加算される`() {
        val productId = "01HXYZPRODUCT0000000000RC05"
        val orderId = "01HXYZORDER00000000000RC05"

        fixture
            .given()
            .events(
                productCreated(productId),
                StockReceivedEvent(productId = productId, receivedQuantity = 10, onHandQuantity = 10),
                // 注文完了で 2 出荷確定 → onHand 10→8。 入庫はこの 8 に加算するのが正しい（10 ではない）。
                StockReservationCommittedEvent(
                    productId = productId,
                    orderId = orderId,
                    quantity = 2,
                    onHandQuantity = 8,
                    reservedQuantity = 0,
                ),
            ).`when`()
            .command(command(productId, 5))
            .then()
            .resultMessagePayload(ReceiveStockCommandResult.success())
            .events(
                StockReceivedEvent(productId = productId, receivedQuantity = 5, onHandQuantity = 13),
            )
    }

    @Test
    fun `異常系_商品が存在しなければ productNotFound`() {
        val productId = "01HXYZPRODUCT0000000000RC03"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(productId, 1))
            .then()
            .resultMessagePayload(ReceiveStockCommandResult.productNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_入庫すると上限を超えるなら quantityOverflow`() {
        val productId = "01HXYZPRODUCT0000000000RC04"

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
            .command(command(productId, 1))
            .then()
            .resultMessagePayload(ReceiveStockCommandResult.quantityOverflow())
            .noEvents()
    }
}
