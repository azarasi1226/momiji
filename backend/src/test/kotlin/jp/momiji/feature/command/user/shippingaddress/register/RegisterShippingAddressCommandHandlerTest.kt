package jp.momiji.feature.command.user.shippingaddress.register

import com.github.michaelbull.result.get
import jp.momiji.MomijiIntegrationTestBase
import jp.momiji.domain.user.Building
import jp.momiji.domain.user.City
import jp.momiji.domain.user.DeliveryNote
import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.PostalCode
import jp.momiji.domain.user.Prefecture
import jp.momiji.domain.user.StreetAddress
import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent
import jp.momiji.event.user.UserCreatedEvent
import org.junit.jupiter.api.Test

class RegisterShippingAddressCommandHandlerTest : MomijiIntegrationTestBase() {
    private fun command(
        userId: String,
        id: String,
        makeDefault: Boolean = false,
    ) = RegisterShippingAddressCommand(
        userId = userId,
        id = id,
        name = Name.create("受取 太郎").get()!!,
        phoneNumber = PhoneNumber.create("090-1234-5678").get()!!,
        postalCode = PostalCode.create("150-0041").get()!!,
        prefecture = Prefecture.create("東京都").get()!!,
        city = City.create("渋谷区").get()!!,
        streetAddress = StreetAddress.create("神南1-2-3").get()!!,
        building = Building.create("momijiビル 4F").get()!!,
        deliveryNote = DeliveryNote.create("置き配可").get()!!,
        makeDefault = makeDefault,
    )

    private fun registeredEvent(
        userId: String,
        id: String,
    ) = ShippingAddressRegisteredEvent(
        userId = userId,
        shippingAddressId = id,
        name = "受取 太郎",
        phoneNumber = "090-1234-5678",
        postalCode = "150-0041",
        prefecture = "東京都",
        city = "渋谷区",
        streetAddress = "神南1-2-3",
        building = "momijiビル 4F",
        deliveryNote = "置き配可",
    )

    private fun defaultChangedEvent(
        userId: String,
        id: String,
    ) = DefaultShippingAddressChangedEvent(userId = userId, shippingAddressId = id)

    @Test
    fun `正常系_初回の配送先はRegisteredとDefaultChangedの2イベントでデフォルトになる`() {
        val userId = "01HXYZSHIPREG000000000000U1"
        val addressId = "01HXYZSHIPREG000000000000A1"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "ship1@example.com"))
            .`when`()
            .command(command(userId, addressId))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.success())
            .events(
                registeredEvent(userId, addressId),
                defaultChangedEvent(userId, addressId),
            )
    }

    @Test
    fun `2件目の配送先はRegisteredのみでデフォルトにならない`() {
        val userId = "01HXYZSHIPREG000000000000U2"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "ship2@example.com"),
                registeredEvent(userId, "01HXYZSHIPREG00000000000A2A"),
                defaultChangedEvent(userId, "01HXYZSHIPREG00000000000A2A"),
            ).`when`()
            .command(command(userId, "01HXYZSHIPREG00000000000A2B"))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.success())
            .events(registeredEvent(userId, "01HXYZSHIPREG00000000000A2B"))
    }

    @Test
    fun `makeDefault指定の2件目はRegisteredとDefaultChangedの2イベントが出る`() {
        val userId = "01HXYZSHIPREG000000000000U3"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "ship3@example.com"),
                registeredEvent(userId, "01HXYZSHIPREG00000000000A3A"),
                defaultChangedEvent(userId, "01HXYZSHIPREG00000000000A3A"),
            ).`when`()
            .command(command(userId, "01HXYZSHIPREG00000000000A3B", makeDefault = true))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.success())
            .events(
                registeredEvent(userId, "01HXYZSHIPREG00000000000A3B"),
                defaultChangedEvent(userId, "01HXYZSHIPREG00000000000A3B"),
            )
    }

    @Test
    fun `冪等性_同じidで再送しても新規イベントを出さず成功`() {
        val userId = "01HXYZSHIPREG000000000000U4"
        val addressId = "01HXYZSHIPREG000000000000A4"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "ship4@example.com"),
                registeredEvent(userId, addressId),
                defaultChangedEvent(userId, addressId),
            ).`when`()
            .command(command(userId, addressId))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.success())
            .noEvents()
    }

    @Test
    fun `上限件数を超えると登録できない`() {
        val userId = "01HXYZSHIPREG000000000000U5"
        val existing =
            (1..RegisterShippingAddressCommandHandler.MAX_ADDRESS_COUNT).map { index ->
                registeredEvent(userId, "01HXYZSHIPREG0000000000A5%02d".format(index))
            }

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "ship5@example.com"),
                *existing.toTypedArray(),
                defaultChangedEvent(userId, "01HXYZSHIPREG0000000000A501"),
            ).`when`()
            .command(command(userId, "01HXYZSHIPREG00000000000A5X"))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.maxCountOver())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZSHIPREG000000000000U6"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(userId, "01HXYZSHIPREG000000000000A6"))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.userNotFound())
            .noEvents()
    }

    @Test
    fun `default不在の状態で登録すると新しい配送先がdefaultになる（自己修復）`() {
        val userId = "01HXYZSHIPREG000000000000U7"

        // default 不在の履歴（Registered のみで DefaultChanged が無い）を生イベントで構築。
        // 現行ハンドラは作らない状態だが、 旧履歴・将来の経路への防御として自己修復を固定する。
        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "ship7@example.com"),
                registeredEvent(userId, "01HXYZSHIPREG00000000000A7A"),
            ).`when`()
            .command(command(userId, "01HXYZSHIPREG00000000000A7B"))
            .then()
            .resultMessagePayload(RegisterShippingAddressCommandResult.success())
            .events(
                registeredEvent(userId, "01HXYZSHIPREG00000000000A7B"),
                defaultChangedEvent(userId, "01HXYZSHIPREG00000000000A7B"),
            )
    }
}
