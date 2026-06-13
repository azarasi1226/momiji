package jp.momiji.feature.command.user.shippingaddress.delete

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.ShippingAddressDeletedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.feature.command.user.shippingaddress.ShippingAddressTestFixtures.defaultChangedEvent
import jp.momiji.feature.command.user.shippingaddress.ShippingAddressTestFixtures.registeredEvent
import org.junit.jupiter.api.Test

class DeleteShippingAddressCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_デフォルトでない配送先の削除は昇格なしの1イベント`() {
        val userId = "01HXYZSHIPDEL000000000000U1"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "del1@example.com"),
                registeredEvent(userId, "01HXYZSHIPDEL00000000000A1A"),
                defaultChangedEvent(userId, "01HXYZSHIPDEL00000000000A1A"),
                registeredEvent(userId, "01HXYZSHIPDEL00000000000A1B"),
            ).`when`()
            .command(DeleteShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEL00000000000A1B"))
            .then()
            .resultMessagePayload(DeleteShippingAddressCommandResult.success())
            .events(ShippingAddressDeletedEvent(userId = userId, shippingAddressId = "01HXYZSHIPDEL00000000000A1B"))
    }

    @Test
    fun `デフォルトを削除すると最古の残り配送先が昇格する`() {
        val userId = "01HXYZSHIPDEL000000000000U2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "del2@example.com"),
                registeredEvent(userId, "01HXYZSHIPDEL00000000000A2A"),
                defaultChangedEvent(userId, "01HXYZSHIPDEL00000000000A2A"),
                registeredEvent(userId, "01HXYZSHIPDEL00000000000A2B"),
                registeredEvent(userId, "01HXYZSHIPDEL00000000000A2C"),
            ).`when`()
            .command(DeleteShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEL00000000000A2A"))
            .then()
            .resultMessagePayload(DeleteShippingAddressCommandResult.success())
            .events(
                ShippingAddressDeletedEvent(userId = userId, shippingAddressId = "01HXYZSHIPDEL00000000000A2A"),
                // 最古の残り（B）が昇格する
                defaultChangedEvent(userId, "01HXYZSHIPDEL00000000000A2B"),
            )
    }

    @Test
    fun `最後の1件を削除すると昇格なしの1イベント`() {
        val userId = "01HXYZSHIPDEL000000000000U3"
        val addressId = "01HXYZSHIPDEL000000000000A3"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "del3@example.com"),
                registeredEvent(userId, addressId),
                defaultChangedEvent(userId, addressId),
            ).`when`()
            .command(DeleteShippingAddressCommand(userId = userId, shippingAddressId = addressId))
            .then()
            .resultMessagePayload(DeleteShippingAddressCommandResult.success())
            .events(ShippingAddressDeletedEvent(userId = userId, shippingAddressId = addressId))
    }

    @Test
    fun `冪等性_存在しない配送先の削除はno-opで成功`() {
        val userId = "01HXYZSHIPDEL000000000000U4"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "del4@example.com"))
            .`when`()
            .command(DeleteShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEL00000000000A4X"))
            .then()
            .resultMessagePayload(DeleteShippingAddressCommandResult.success())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZSHIPDEL000000000000U5"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(DeleteShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEL000000000000A5"))
            .then()
            .resultMessagePayload(DeleteShippingAddressCommandResult.userNotFound())
            .noEvents()
    }
}
