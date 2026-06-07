package jp.momiji.feature.command.basket.setitem

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.basket.BasketItemQuantity
import jp.momiji.event.basket.BasketItemSetEvent
import jp.momiji.event.product.ProductCreatedEvent
import jp.momiji.event.product.ProductDiscontinuedEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class SetBasketItemCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。
    private fun userCreated(userId: String) = UserCreatedEvent(id = userId, email = "user@example.com")

    private fun productCreated(productId: String) =
        ProductCreatedEvent(
            id = productId,
            brandId = "01HXYZBRAND0000000000000B1",
            name = "テスト商品",
            description = "説明",
            imageUrl = null,
            price = 1000,
        )

    private fun command(
        userId: String,
        productId: String,
        quantity: Int = 2,
    ) = SetBasketItemCommand(
        userId = userId,
        productId = productId,
        itemQuantity = BasketItemQuantity.create(quantity).get()!!,
    )

    @Test
    fun `正常系_user存在かつproductがACTIVEならカゴにセット`() {
        val userId = "01HXYZUSER000000000000SET01"
        val productId = "01HXYZPRODUCT0000000000ST01"

        fixture
            .given()
            .events(userCreated(userId), productCreated(productId))
            .`when`()
            .command(command(userId, productId, quantity = 3))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.success())
            .events(
                BasketItemSetEvent(userId = userId, productId = productId, itemQuantity = 3),
            )
    }

    @Test
    fun `異常系_userが存在しなければ userNotFound`() {
        val userId = "01HXYZUSER000000000000SET02"
        val productId = "01HXYZPRODUCT0000000000ST02"

        fixture
            .given()
            .events(productCreated(productId))
            .`when`()
            .command(command(userId, productId))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_productが存在しなければ productNotFound`() {
        val userId = "01HXYZUSER000000000000SET03"
        val productId = "01HXYZPRODUCT0000000000ST03"

        fixture
            .given()
            .events(userCreated(userId))
            .`when`()
            .command(command(userId, productId))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.productNotFound())
            .noEvents()
    }

    @Test
    fun `異常系_productが生産終了なら productNotFound`() {
        val userId = "01HXYZUSER000000000000SET04"
        val productId = "01HXYZPRODUCT0000000000ST04"

        fixture
            .given()
            .events(userCreated(userId), productCreated(productId), ProductDiscontinuedEvent(id = productId))
            .`when`()
            .command(command(userId, productId))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.productNotFound())
            .noEvents()
    }

    @Test
    fun `正常系_既にカゴにある商品の個数変更は上限に数えず成功`() {
        val userId = "01HXYZUSER000000000000SET05"
        val productId = "01HXYZPRODUCT0000000000ST05"

        fixture
            .given()
            .events(
                userCreated(userId),
                productCreated(productId),
                BasketItemSetEvent(userId = userId, productId = productId, itemQuantity = 1),
            ).`when`()
            .command(command(userId, productId, quantity = 5))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.success())
            .events(
                BasketItemSetEvent(userId = userId, productId = productId, itemQuantity = 5),
            )
    }

    @Test
    fun `冪等性_既にカゴにある商品を同じ個数で再セットしてもイベントを出さず成功`() {
        val userId = "01HXYZUSER000000000000SET07"
        val productId = "01HXYZPRODUCT0000000000ST07"

        fixture
            .given()
            .events(
                userCreated(userId),
                productCreated(productId),
                BasketItemSetEvent(userId = userId, productId = productId, itemQuantity = 4),
            ).`when`()
            .command(command(userId, productId, quantity = 4))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.success())
            .noEvents()
    }

    @Test
    fun `異常系_新規商品でカゴの種類数が上限に達していれば productMaxKindOver`() {
        val userId = "01HXYZUSER000000000000SET06"
        val productId = "01HXYZPRODUCT0000000000ST06"

        // user + 対象 product + 既に MAX 種類の別商品がカゴにある状態を作る
        val priorItems =
            (0 until SetBasketItemCommandHandler.MAX_ITEM_KIND_COUNT).map { i ->
                BasketItemSetEvent(
                    userId = userId,
                    productId = "01HXYZPRODUCTFILL06%07d".format(i),
                    itemQuantity = 1,
                )
            }
        val priorEvents = listOf(userCreated(userId), productCreated(productId)) + priorItems

        fixture
            .given()
            .events(*priorEvents.toTypedArray())
            .`when`()
            .command(command(userId, productId))
            .then()
            .resultMessagePayload(SetBasketItemCommandResult.productMaxKindOver())
            .noEvents()
    }
}
