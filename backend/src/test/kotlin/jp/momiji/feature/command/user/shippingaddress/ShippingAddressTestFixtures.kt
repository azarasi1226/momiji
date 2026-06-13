package jp.momiji.feature.command.user.shippingaddress

import jp.momiji.event.user.DefaultShippingAddressChangedEvent
import jp.momiji.event.user.ShippingAddressRegisteredEvent

/**
 * 配送先テスト共通のイベント生成ヘルパ（register/update/delete/changedefault の各テストで共有）。
 */
object ShippingAddressTestFixtures {
    fun registeredEvent(
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

    fun defaultChangedEvent(
        userId: String,
        id: String,
    ) = DefaultShippingAddressChangedEvent(userId = userId, shippingAddressId = id)
}
