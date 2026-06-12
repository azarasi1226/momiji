/** フォームの field 別バリデーションエラー表示。 message が無ければ何も描画しない。 */
export function FieldError({ message }: { message?: string }) {
  if (!message) return null
  return <p className="text-xs text-destructive">{message}</p>
}
