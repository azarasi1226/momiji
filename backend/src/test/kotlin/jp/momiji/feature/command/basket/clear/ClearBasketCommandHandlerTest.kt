package jp.momiji.feature.command.basket.clear

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.basket.BasketClearedEvent
import jp.momiji.event.basket.BasketItemSetEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class ClearBasketCommandHandlerTest : MomijiIntegrationTestBase() {
    // EventStore は全テストで共有・リセットされないため、テストごとにユニークな id を使う。
    private fun userCreated(userId: String) = UserCreatedEvent(id = userId, email = "user@example.com")

    @Test
    fun `正常系_中身のあるカゴを空にすると BasketClearedEvent`() {
        val userId = "01HXYZUSER000000000000CLR01"

        fixture
            .given()
            .events(
                userCreated(userId),
                BasketItemSetEvent(userId = userId, productId = "01HXYZPRODUCT0000000000CL01", itemQuantity = 1),
                BasketItemSetEvent(userId = userId, productId = "01HXYZPRODUCT0000000000CL02", itemQuantity = 2),
            ).`when`()
            .command(ClearBasketCommand(userId = userId))
            .then()
            .resultMessagePayload(ClearBasketCommandResult.success())
            .events(
                BasketClearedEvent(userId = userId),
            )
    }

    @Test
    fun `異常系_userが存在しなければ userNotFound`() {
        val userId = "01HXYZUSER000000000000CLR02"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(ClearBasketCommand(userId = userId))
            .then()
            .resultMessagePayload(ClearBasketCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `冪等性_既に空のカゴをクリアしても新規イベントを出さず成功`() {
        val userId = "01HXYZUSER000000000000CLR03"

        fixture
            .given()
            .events(userCreated(userId))
            .`when`()
            .command(ClearBasketCommand(userId = userId))
            .then()
            .resultMessagePayload(ClearBasketCommandResult.success())
            .noEvents()
    }
}
