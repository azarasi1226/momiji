"use client"

import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { PhoneNumberFields, PostalCodeFields } from "@/components/form/digit-fields"
import { PREFECTURES } from "@/lib/prefectures"
import { lookupAddress, type ShippingAddress } from "./actions"
import { FieldError } from "@/components/form/field-error"

/**
 * 配送先フォームの共通フィールド（追加・編集で共有）。
 * 同一ページに複数フォームが並ぶため、 id は idPrefix で衝突を避ける（name は FormData 用にそのまま）。
 *
 * 郵便番号が 7 桁揃うと zipcloud で住所を引き、 都道府県・市区町村・番地（町域、 空のときのみ）を自動入力する。
 * 失敗時は何もしない（手入力フォールバック）。
 */
export function AddressFormFields({
  idPrefix,
  defaults,
  fieldErrors,
}: {
  idPrefix: string
  defaults?: Partial<ShippingAddress>
  fieldErrors?: Record<string, string>
}) {
  async function onPostalComplete(postalCode: string) {
    const result = await lookupAddress(postalCode)
    if (!result) return
    const setValue = (id: string, value: string, onlyIfEmpty = false) => {
      const element = document.getElementById(`${idPrefix}${id}`) as HTMLInputElement | HTMLSelectElement | null
      if (!element) return
      if (onlyIfEmpty && element.value !== "") return
      element.value = value
    }
    setValue("prefecture", result.prefecture)
    setValue("city", result.city)
    // 番地はユーザーが番・号を追記する欄なので、 既に入力があるときは上書きしない
    setValue("streetAddress", result.town, true)
  }

  return (
    <>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}name`}>受取人氏名</Label>
        <Input
          id={`${idPrefix}name`}
          name="name"
          type="text"
          defaultValue={defaults?.name ?? ""}
          required
          aria-invalid={!!fieldErrors?.name}
        />
        <FieldError message={fieldErrors?.name} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}phoneNumber1`}>電話番号（ドライバー連絡用）</Label>
        <PhoneNumberFields
          idPrefix={idPrefix}
          defaultValue={defaults?.phoneNumber ?? ""}
          invalid={!!fieldErrors?.phoneNumber}
        />
        <FieldError message={fieldErrors?.phoneNumber} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}postalCode1`}>郵便番号</Label>
        <PostalCodeFields
          idPrefix={idPrefix}
          defaultValue={defaults?.postalCode ?? ""}
          invalid={!!fieldErrors?.postalCode}
          onComplete={onPostalComplete}
        />
        <p className="text-xs text-muted-foreground">7桁入力すると住所を自動入力します</p>
        <FieldError message={fieldErrors?.postalCode} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}prefecture`}>都道府県</Label>
        <select
          id={`${idPrefix}prefecture`}
          name="prefecture"
          defaultValue={defaults?.prefecture ?? ""}
          required
          aria-invalid={!!fieldErrors?.prefecture}
          className="h-9 w-40 rounded-md border border-input bg-transparent px-3 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <option value="" disabled>
            選択してください
          </option>
          {PREFECTURES.map((prefecture) => (
            <option key={prefecture} value={prefecture}>
              {prefecture}
            </option>
          ))}
        </select>
        <FieldError message={fieldErrors?.prefecture} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}city`}>市区町村</Label>
        <Input
          id={`${idPrefix}city`}
          name="city"
          type="text"
          defaultValue={defaults?.city ?? ""}
          required
          aria-invalid={!!fieldErrors?.city}
        />
        <FieldError message={fieldErrors?.city} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}streetAddress`}>番地</Label>
        <Input
          id={`${idPrefix}streetAddress`}
          name="streetAddress"
          type="text"
          defaultValue={defaults?.streetAddress ?? ""}
          required
          aria-invalid={!!fieldErrors?.streetAddress}
        />
        <FieldError message={fieldErrors?.streetAddress} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}building`}>建物名・部屋番号（任意）</Label>
        <Input
          id={`${idPrefix}building`}
          name="building"
          type="text"
          defaultValue={defaults?.building ?? ""}
          aria-invalid={!!fieldErrors?.building}
        />
        <FieldError message={fieldErrors?.building} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor={`${idPrefix}deliveryNote`}>配達メモ（任意）</Label>
        <Input
          id={`${idPrefix}deliveryNote`}
          name="deliveryNote"
          type="text"
          placeholder="例: 置き配可・不在時は宅配ボックスへ"
          defaultValue={defaults?.deliveryNote ?? ""}
          aria-invalid={!!fieldErrors?.deliveryNote}
        />
        <FieldError message={fieldErrors?.deliveryNote} />
      </div>
    </>
  )
}
