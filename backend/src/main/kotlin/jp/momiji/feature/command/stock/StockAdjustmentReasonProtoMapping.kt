package jp.momiji.feature.command.stock

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError
import jp.momiji.domain.stock.StockAdjustmentReason
import jp.momiji.grpc.momiji.stock.v1.StockAdjustmentReason as ProtoStockAdjustmentReason

/**
 * gRPC の proto enum を調整理由のドメイン enum に変換する。
 * UNSPECIFIED / 未知の値は ValidationError（理由は必須）。
 */
internal fun stockAdjustmentReasonFromProto(proto: ProtoStockAdjustmentReason): Result<StockAdjustmentReason, ValidationError> =
    when (proto) {
        ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_DAMAGED -> Ok(StockAdjustmentReason.DAMAGED)
        ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_LOST -> Ok(StockAdjustmentReason.LOST)
        ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_STOCKTAKING -> Ok(StockAdjustmentReason.STOCKTAKING)
        ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_CORRECTION -> Ok(StockAdjustmentReason.CORRECTION)
        ProtoStockAdjustmentReason.STOCK_ADJUSTMENT_REASON_OTHER -> Ok(StockAdjustmentReason.OTHER)
        else -> Err(ReasonUnspecified)
    }

private object ReasonUnspecified : ValidationError("reason", "調整理由を指定してください")
