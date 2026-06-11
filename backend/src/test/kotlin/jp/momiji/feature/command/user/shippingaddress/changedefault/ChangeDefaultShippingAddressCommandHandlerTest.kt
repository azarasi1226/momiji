package jp.momiji.feature.command.user.shippingaddress.changedefault

import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.feature.command.user.shippingaddress.ShippingAddressTestFixtures.defaultChangedEvent
import jp.momiji.feature.command.user.shippingaddress.ShippingAddressTestFixtures.registeredEvent
import org.junit.jupiter.api.Test

class ChangeDefaultShippingAddressCommandHandlerTest : MomijiIntegrationTestBase() {
    @Test
    fun `正常系_別の配送先をデフォルトに変更できる`() {
        val userId = "01HXYZSHIPDEF000000000000U1"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "def1@example.com"),
                registeredEvent(userId, "01HXYZSHIPDEF00000000000A1A"),
                defaultChangedEvent(userId, "01HXYZSHIPDEF00000000000A1A"),
                registeredEvent(userId, "01HXYZSHIPDEF00000000000A1B"),
            ).`when`()
            .command(
                ChangeDefaultShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEF00000000000A1B"),
            ).then()
            .resultMessagePayload(ChangeDefaultShippingAddressCommandResult.success())
            .events(defaultChangedEvent(userId, "01HXYZSHIPDEF00000000000A1B"))
    }

    @Test
    fun `冪等性_既にデフォルトなら新規イベントを出さず成功`() {
        val userId = "01HXYZSHIPDEF000000000000U2"
        val addressId = "01HXYZSHIPDEF000000000000A2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "def2@example.com"),
                registeredEvent(userId, addressId),
                defaultChangedEvent(userId, addressId),
            ).`when`()
            .command(ChangeDefaultShippingAddressCommand(userId = userId, shippingAddressId = addressId))
            .then()
            .resultMessagePayload(ChangeDefaultShippingAddressCommandResult.success())
            .noEvents()
    }

    @Test
    fun `存在しない配送先はデフォルトにできない`() {
        val userId = "01HXYZSHIPDEF000000000000U3"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "def3@example.com"))
            .`when`()
            .command(
                ChangeDefaultShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEF00000000000A3X"),
            ).then()
            .resultMessagePayload(ChangeDefaultShippingAddressCommandResult.addressNotFound())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZSHIPDEF000000000000U4"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(
                ChangeDefaultShippingAddressCommand(userId = userId, shippingAddressId = "01HXYZSHIPDEF000000000000A4"),
            ).then()
            .resultMessagePayload(ChangeDefaultShippingAddressCommandResult.userNotFound())
            .noEvents()
    }
}
