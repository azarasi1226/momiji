package jp.momiji.feature.command.user.shippingaddress.update

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
import jp.momiji.event.user.ShippingAddressUpdatedEvent
import jp.momiji.event.user.UserCreatedEvent
import jp.momiji.feature.command.user.shippingaddress.ShippingAddressTestFixtures.defaultChangedEvent
import jp.momiji.feature.command.user.shippingaddress.ShippingAddressTestFixtures.registeredEvent
import org.junit.jupiter.api.Test

class UpdateShippingAddressCommandHandlerTest : MomijiIntegrationTestBase() {
    private fun command(
        userId: String,
        id: String,
    ) = UpdateShippingAddressCommand(
        userId = userId,
        shippingAddressId = id,
        name = Name.create("受取 花子").get()!!,
        phoneNumber = PhoneNumber.create("080-9999-0000").get()!!,
        postalCode = PostalCode.create("220-0012").get()!!,
        prefecture = Prefecture.create("神奈川県").get()!!,
        city = City.create("横浜市西区").get()!!,
        streetAddress = StreetAddress.create("みなとみらい4-5-6").get()!!,
        building = Building.create("").get()!!,
        deliveryNote = DeliveryNote.create("インターホン不要").get()!!,
    )

    @Test
    fun `正常系_編集後の全フィールドを持つUpdatedイベントが出る`() {
        val userId = "01HXYZSHIPUPD000000000000U1"
        val addressId = "01HXYZSHIPUPD000000000000A1"

        fixture
            .given()
            .events(
                UserCreatedEvent(id = userId, email = "upd1@example.com"),
                registeredEvent(userId, addressId),
                defaultChangedEvent(userId, addressId),
            ).`when`()
            .command(command(userId, addressId))
            .then()
            .resultMessagePayload(UpdateShippingAddressCommandResult.success())
            .events(
                ShippingAddressUpdatedEvent(
                    userId = userId,
                    shippingAddressId = addressId,
                    name = "受取 花子",
                    phoneNumber = "080-9999-0000",
                    postalCode = "220-0012",
                    prefecture = "神奈川県",
                    city = "横浜市西区",
                    streetAddress = "みなとみらい4-5-6",
                    building = "",
                    deliveryNote = "インターホン不要",
                ),
            )
    }

    @Test
    fun `存在しない配送先は編集できない`() {
        val userId = "01HXYZSHIPUPD000000000000U2"

        fixture
            .given()
            .events(UserCreatedEvent(id = userId, email = "upd2@example.com"))
            .`when`()
            .command(command(userId, "01HXYZSHIPUPD00000000000A2X"))
            .then()
            .resultMessagePayload(UpdateShippingAddressCommandResult.addressNotFound())
            .noEvents()
    }

    @Test
    fun `ユーザーが存在しないと失敗`() {
        val userId = "01HXYZSHIPUPD000000000000U3"

        fixture
            .given()
            .noPriorActivity()
            .`when`()
            .command(command(userId, "01HXYZSHIPUPD000000000000A3"))
            .then()
            .resultMessagePayload(UpdateShippingAddressCommandResult.userNotFound())
            .noEvents()
    }
}
