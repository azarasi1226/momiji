/**
 * ISO 文字列（UTC の絶対時刻）を **日本時間**の表示文字列にする。
 *
 * backend は wire で常に UTC 絶対時刻（proto Timestamp）を返す。 表示のタイムゾーンはフロントの責務なので、
 * ここで明示的に `Asia/Tokyo` に変換する（実行環境の TZ に依存させない）。
 */
export function formatDateTime(iso: string): string {
  if (!iso) return "—"
  return new Date(iso).toLocaleString("ja-JP", { timeZone: "Asia/Tokyo" })
}
