package jp.momiji.projection.stock

import io.github.oshai.kotlinlogging.KotlinLogging
import iss.jooq.generated.tables.references.STOCKS
import jp.momiji.event.stock.StockAdjustedEvent
import jp.momiji.event.stock.StockReceivedEvent
import jp.momiji.event.stock.StockReservationCommittedEvent
import jp.momiji.event.stock.StockReservationReleasedEvent
import jp.momiji.event.stock.StockReservedEvent
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

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
        event: StockReservedEvent,
        @Timestamp timestamp: Instant,
    ) {
        setReserved(event.productId, event.reservedQuantity, timestamp)
    }

    @EventHandler
    fun on(
        event: StockReservationReleasedEvent,
        @Timestamp timestamp: Instant,
    ) {
        setReserved(event.productId, event.reservedQuantity, timestamp)
    }

    @EventHandler
    fun on(
        event: StockReservationCommittedEvent,
        @Timestamp timestamp: Instant,
    ) {
        // 出荷確定: onHand・reserved とも絶対値で上書き（出荷した分 onHand が減る）。
        val at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC)
        val updated =
            dsl
                .update(STOCKS)
                .set(STOCKS.ON_HAND, event.onHandQuantity)
                .set(STOCKS.RESERVED, event.reservedQuantity)
                .set(STOCKS.UPDATED_AT, at)
                .where(STOCKS.PRODUCT_ID.eq(event.productId))
                .execute()
        if (updated == 0) {
            logger.warn { "出荷確定の反映先の在庫行がありません: productId=${event.productId}" }
        }
    }

    // reservedQuantity は予約数の絶対値なので set（差分でなく上書き）。 予約・解放どちらも再処理して冪等。
    private fun setReserved(
        productId: String,
        reservedQuantity: Int,
        timestamp: Instant,
    ) {
        val at = LocalDateTime.ofInstant(timestamp, ZoneOffset.UTC)
        val updated =
            dsl
                .update(STOCKS)
                .set(STOCKS.RESERVED, reservedQuantity)
                .set(STOCKS.UPDATED_AT, at)
                .where(STOCKS.PRODUCT_ID.eq(productId))
                .execute()
        // 予約/解放はコマンド側で確認済みなので在庫行は必ずあるはず。 規約: 対象不在は warn。
        if (updated == 0) {
            logger.warn { "予約数の反映先の在庫行がありません: productId=$productId" }
        }
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
