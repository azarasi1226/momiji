// 分割枠（phoneNumber1〜3 / postalCode1〜2）をハイフン結合して backend の保存形式に合わせる。
// server action 側で使う（client の分割枠コンポーネントは components/form/digit-fields.tsx）。

export function joinPhoneNumber(formData: FormData): string {
  return ["phoneNumber1", "phoneNumber2", "phoneNumber3"]
    .map((key) => (formData.get(key) as string) ?? "")
    .join("-");
}

export function joinPostalCode(formData: FormData): string {
  return ["postalCode1", "postalCode2"]
    .map((key) => (formData.get(key) as string) ?? "")
    .join("-");
}
