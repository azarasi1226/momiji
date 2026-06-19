package jp.momiji.feature.query.user.shippingaddress.listmyshippingaddresses

import iss.jooq.generated.tables.references.SHIPPING_ADDRESSES
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ListMyShippingAddressesQueryService(
    private val dsl: DSLContext,
) {
    fun findByUserId(userId: String): List<ShippingAddressView> =
        dsl
            .select(
                SHIPPING_ADDRESSES.ID,
                SHIPPING_ADDRESSES.NAME,
                SHIPPING_ADDRESSES.PHONE_NUMBER,
                SHIPPING_ADDRESSES.POSTAL_CODE,
                SHIPPING_ADDRESSES.PREFECTURE,
                SHIPPING_ADDRESSES.CITY,
                SHIPPING_ADDRESSES.STREET_ADDRESS,
                SHIPPING_ADDRESSES.BUILDING,
                SHIPPING_ADDRESSES.DELIVERY_NOTE,
                SHIPPING_ADDRESSES.IS_DEFAULT,
            ).from(SHIPPING_ADDRESSES)
            .where(SHIPPING_ADDRESSES.USER_ID.eq(userId))
            // 登録順で固定する。 is_default でソートすると default 変更のたびに行が入れ替わり、
            // 変化が「バッジの移動」でなく「行の入替」に見えて分かりにくい（保存カード一覧と同じ判断）。
            .orderBy(SHIPPING_ADDRESSES.CREATED_AT.asc())
            .fetch { record ->
                ShippingAddressView(
                    id = record[SHIPPING_ADDRESSES.ID]!!,
                    name = record[SHIPPING_ADDRESSES.NAME]!!,
                    phoneNumber = record[SHIPPING_ADDRESSES.PHONE_NUMBER]!!,
                    postalCode = record[SHIPPING_ADDRESSES.POSTAL_CODE]!!,
                    prefecture = record[SHIPPING_ADDRESSES.PREFECTURE]!!,
                    city = record[SHIPPING_ADDRESSES.CITY]!!,
                    streetAddress = record[SHIPPING_ADDRESSES.STREET_ADDRESS]!!,
                    building = record[SHIPPING_ADDRESSES.BUILDING]!!,
                    deliveryNote = record[SHIPPING_ADDRESSES.DELIVERY_NOTE]!!,
                    isDefault = record[SHIPPING_ADDRESSES.IS_DEFAULT]!!,
                )
            }
}
