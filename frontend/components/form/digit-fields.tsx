"use client";

import { Input } from "@/components/ui/input";

// 数字以外を弾き、 枠が埋まったら次の枠へ自動フォーカスする（電話番号・郵便番号の分割枠用）。
function digitsAutoAdvance(
  e: React.FormEvent<HTMLInputElement>,
  nextId?: string,
) {
  const input = e.currentTarget;
  input.value = input.value.replace(/\D/g, "");
  if (nextId && input.value.length >= input.maxLength) {
    document.getElementById(nextId)?.focus();
  }
}

/**
 * 電話番号の分割枠（市外局番 - 市内局番 - 加入者番号）。
 * name は phoneNumber1〜3 固定。 server action 側でハイフン結合して backend の保存形式に合わせる。
 * 同一ページに複数フォームを置く場合は idPrefix で id 衝突を避ける。
 */
export function PhoneNumberFields({
  idPrefix = "",
  defaultValue = "",
  invalid = false,
}: {
  idPrefix?: string;
  defaultValue?: string;
  invalid?: boolean;
}) {
  const [p1 = "", p2 = "", p3 = ""] = defaultValue.split("-");
  const id = (n: number) => `${idPrefix}phoneNumber${n}`;

  return (
    <div className="flex items-center gap-2">
      <Input
        id={id(1)}
        name="phoneNumber1"
        type="tel"
        inputMode="numeric"
        maxLength={4}
        className="w-20 text-center"
        defaultValue={p1}
        required
        aria-invalid={invalid}
        aria-label="電話番号（市外局番）"
        onInput={(e) => digitsAutoAdvance(e, id(2))}
      />
      <span className="text-muted-foreground">-</span>
      <Input
        id={id(2)}
        name="phoneNumber2"
        type="tel"
        inputMode="numeric"
        maxLength={4}
        className="w-20 text-center"
        defaultValue={p2}
        required
        aria-invalid={invalid}
        aria-label="電話番号（市内局番）"
        onInput={(e) => digitsAutoAdvance(e, id(3))}
      />
      <span className="text-muted-foreground">-</span>
      <Input
        id={id(3)}
        name="phoneNumber3"
        type="tel"
        inputMode="numeric"
        maxLength={4}
        className="w-20 text-center"
        defaultValue={p3}
        required
        aria-invalid={invalid}
        aria-label="電話番号（加入者番号）"
        onInput={(e) => digitsAutoAdvance(e)}
      />
    </div>
  );
}

/**
 * 郵便番号の分割枠（前3桁 - 後4桁）。 name は postalCode1〜2 固定。
 * [onComplete] を渡すと、 7 桁揃った瞬間に結合済み 7 桁（ハイフンなし）で呼ばれる（住所自動補完のトリガー用）。
 */
export function PostalCodeFields({
  idPrefix = "",
  defaultValue = "",
  invalid = false,
  onComplete,
}: {
  idPrefix?: string;
  defaultValue?: string;
  invalid?: boolean;
  onComplete?: (postalCode: string) => void;
}) {
  const [z1 = "", z2 = ""] = defaultValue.split("-");
  const id = (n: number) => `${idPrefix}postalCode${n}`;

  function handleInput(e: React.FormEvent<HTMLInputElement>, nextId?: string) {
    digitsAutoAdvance(e, nextId);
    if (!onComplete) return;
    const v1 =
      (document.getElementById(id(1)) as HTMLInputElement | null)?.value ?? "";
    const v2 =
      (document.getElementById(id(2)) as HTMLInputElement | null)?.value ?? "";
    if (v1.length === 3 && v2.length === 4) {
      onComplete(v1 + v2);
    }
  }

  return (
    <div className="flex items-center gap-2">
      <Input
        id={id(1)}
        name="postalCode1"
        type="text"
        inputMode="numeric"
        maxLength={3}
        className="w-16 text-center"
        defaultValue={z1}
        required
        aria-invalid={invalid}
        aria-label="郵便番号（前3桁）"
        onInput={(e) => handleInput(e, id(2))}
      />
      <span className="text-muted-foreground">-</span>
      <Input
        id={id(2)}
        name="postalCode2"
        type="text"
        inputMode="numeric"
        maxLength={4}
        className="w-20 text-center"
        defaultValue={z2}
        required
        aria-invalid={invalid}
        aria-label="郵便番号（後4桁）"
        onInput={(e) => handleInput(e)}
      />
    </div>
  );
}
