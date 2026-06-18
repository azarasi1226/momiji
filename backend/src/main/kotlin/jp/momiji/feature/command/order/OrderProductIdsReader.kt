package jp.momiji.feature.command.order

import iss.jooq.generated.tables.references.ORDER_ITEMS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

/**
 * 注文の product_id を read model（order_items）から取得する共有リーダー。
 *
 * Fail / Complete のドライバ（OrderExpirySweeper / OrderPaymentFailedWebhookHandler / CompleteShippedOrderProcessManager）が
 * 在庫操作コマンドの整合境界（product_id 集合）を組むのに使う。 order_items は OrderStarted 時点で投影済みである前提
 * （決済準備の PayableOrderReader がアトミックに product_id を確認しているので、 そこを通った注文は必ず投影済み）。
 */
@Component
class OrderProductIdsReader(
    private val dsl: DSLContext,
) {
    fun read(orderId: String): List<String> =
        dsl
            .select(ORDER_ITEMS.PRODUCT_ID)
            .from(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetch(ORDER_ITEMS.PRODUCT_ID)
            .filterNotNull()
}
