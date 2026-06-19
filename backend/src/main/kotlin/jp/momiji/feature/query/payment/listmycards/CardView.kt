package jp.momiji.feature.query.payment.listmycards

data class CardView(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val isDefault: Boolean,
)
