package jp.momiji.domain.stock

/**
 * 在庫調整の理由。 販売による減少は予約→確定フローで扱うので、 ここには**販売以外**の増減理由だけを並べる。
 *
 * 監査で「なぜ在庫が動いたか」を残すために調整は必ず理由を持つ。
 *
 * [canIncrease] は「この理由で在庫を**増やせる**か（プラス調整できるか）」。 破損・紛失などは減るだけなので false、
 * 棚卸し差異だけが増減どちらもありうる。 増加を許す組み合わせかは [StockAdjustment] が検証する。
 */
enum class StockAdjustmentReason(
    val canIncrease: Boolean,
) {
    /** 破損・故障で廃棄（減るだけ） */
    DAMAGED(canIncrease = false),

    /** 紛失・盗難（減るだけ） */
    LOST(canIncrease = false),

    /** 棚卸し差異の修正（増減どちらもありうる） */
    STOCKTAKING(canIncrease = true),

    /** 入力ミス等の訂正（減るだけ） */
    CORRECTION(canIncrease = false),

    /** その他（減るだけ） */
    OTHER(canIncrease = false),
}
