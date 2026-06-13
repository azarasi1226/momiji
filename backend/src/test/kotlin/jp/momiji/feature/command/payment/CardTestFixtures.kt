package jp.momiji.feature.command.payment

import jp.momiji.event.payment.CardRegisteredEvent
import jp.momiji.event.payment.DefaultCardChangedEvent

/**
 * カードテスト共通のイベント生成ヘルパ（recordcard/deletecard/changedefaultcard の各テストで共有）。
 */
object CardTestFixtures {
    fun registeredEvent(
        userId: String,
        paymentMethodId: String,
        brand: String = "visa",
        last4: String = "4242",
        expMonth: Int = 1,
        expYear: Int = 2030,
    ) = CardRegisteredEvent(
        userId = userId,
        paymentMethodId = paymentMethodId,
        brand = brand,
        last4 = last4,
        expMonth = expMonth,
        expYear = expYear,
    )

    fun defaultChangedEvent(
        userId: String,
        paymentMethodId: String,
    ) = DefaultCardChangedEvent(userId = userId, paymentMethodId = paymentMethodId)
}
