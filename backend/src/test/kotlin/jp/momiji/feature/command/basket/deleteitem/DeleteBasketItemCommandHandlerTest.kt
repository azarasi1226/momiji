package jp.momiji.feature.command.basket.deleteitem

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.basket.BasketItemDeletedEvent
import jp.momiji.event.basket.BasketItemSetEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class DeleteBasketItemCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。
    private fun userCreated(userId: String) = UserCreatedEvent(id = userId, email = "user@example.com")

    @Test
    fun `正常系_カゴにある商品を削除すると BasketItemDeletedEvent`() {
        val userId = "01HXYZUSER000000000000DEL01"
        val productId = "01HXYZPRODUCT0000000000DL01"

        fixture
            .given()
            .events(
                userCreated(userId),
                BasketItemSetEvent(userId = userId, productId = productId, itemQuantity = 2),
            ).`when`()
            .command(DeleteBasketItemCommand(userId = userId, productId = productId))
            .then()
            .resultMessagePayload(DeleteBasketItemCommandResult.success())
            .events(
                BasketItemDeletedEvent(userId = userId, productId = productId),
            )
    }

    @Test
    fun `異常系_userが存在しなければ userNotFound`() {
        val userId = "01HXYZUSER000000000000DEL02"
        val productId = "01HXYZPRODUCT0000000000DL02"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(DeleteBasketItemCommand(userId = userId, productId = productId))
            .then()
            .resultMessagePayload(DeleteBasketItemCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_カゴに無い商品の削除は新規イベントを出さず成功`() {
        val userId = "01HXYZUSER000000000000DEL03"
        val productId = "01HXYZPRODUCT0000000000DL03"

        fixture
            .given()
            .events(userCreated(userId))
            .`when`()
            .command(DeleteBasketItemCommand(userId = userId, productId = productId))
            .then()
            .resultMessagePayload(DeleteBasketItemCommandResult.success())
            .noEvents()
    }
}
