/**
 * status の「正準コード」（ACTIVE / ARCHIVED / DISCONTINUED）を **表示用の日本語ラベル**に変換する。
 *
 * 設計方針:
 * - actions が返す `status` は **正準コード文字列**のまま保つ（バッジ色などのロジックはコードで判定する）。
 * - 画面に出す瞬間だけこのラベル関数を通す。 表示文言の変更がここ 1 か所に閉じる。
 * - 未知コード（UNSPECIFIED 由来の "UNKNOWN" 等）は**握りつぶさずそのまま返す**（取りこぼしに気付ける）。
 */

const BRAND_STATUS_LABELS: Record<string, string> = {
  ACTIVE: "有効",
  ARCHIVED: "アーカイブ済み",
}

const PRODUCT_STATUS_LABELS: Record<string, string> = {
  ACTIVE: "販売中",
  DISCONTINUED: "生産終了",
}

export function brandStatusLabel(status: string): string {
  return BRAND_STATUS_LABELS[status] ?? status
}

export function productStatusLabel(status: string): string {
  return PRODUCT_STATUS_LABELS[status] ?? status
}
