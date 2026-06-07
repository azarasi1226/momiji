package jp.momiji.projection.stock

import iss.jooq.generated.tables.references.STOCKS
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.event.stock.StockReceivedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class StockTableProjector(
    private val dsl: DSLContext,
) {
    @EventHandler
    fun on(
        event: StockReceivedEvent,
        @Timestamp timestamp: Instant,
    ) {
        upsertOnHand(event.productId, event.onHandQuantity, timestamp)
    }

    @EventHandler
    fun on(
        event: StockAdjustedEvent,
        @Timestamp timestamp: Instant,
    ) {
        upsertOnHand(event.productId, event.onHandQuantity, timestamp)
    }

    private fun upsertOnHand(
        productId: String,
        onHand: Int,
        timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC)
        dsl
            .insertInto(STOCKS)
            .set(STOCKS.PRODUCT_ID, productId)
            .set(STOCKS.ON_HAND, onHand)
            // 在庫レコードは暗黙ゼロ（最初の在庫イベントで行ができる）。 onHand は絶対値なので set。
            // reserved は予約フローで更新する（現状は INSERT 時の 0 のまま）
            .set(STOCKS.RESERVED, 0)
            .set(STOCKS.UPDATED_AT, at)
            .onDuplicateKeyUpdate()
            .set(STOCKS.ON_HAND, onHand)
            .set(STOCKS.UPDATED_AT, at)
            .execute()
    }
}
