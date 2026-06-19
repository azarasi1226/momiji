package jp.momiji.feature.query.user.shippingaddress.listmyshippingaddresses

data class ShippingAddressView(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val postalCode: String,
    val prefecture: String,
    val city: String,
    val streetAddress: String,
    val building: String,
    val deliveryNote: String,
    val isDefault: Boolean,
)
