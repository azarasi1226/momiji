"use client"

import { useState, useTransition } from "react"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { AddressFormFields } from "./address-form-fields"
import {
  changeDefaultShippingAddress,
  deleteShippingAddress,
  registerShippingAddress,
  updateShippingAddress,
  type SaveAddressState,
  type ShippingAddress,
} from "./actions"

/** フォームの FormData から楽観表示用の ShippingAddress を組み立てる（分割枠はここで結合）。 */
function addressFromForm(id: string, formData: FormData): ShippingAddress {
  const get = (key: string) => (formData.get(key) as string) ?? ""
  return {
    id,
    name: get("name"),
    phoneNumber: [get("phoneNumber1"), get("phoneNumber2"), get("phoneNumber3")].join("-"),
    postalCode: [get("postalCode1"), get("postalCode2")].join("-"),
    prefecture: get("prefecture"),
    city: get("city"),
    streetAddress: get("streetAddress"),
    building: get("building"),
    deliveryNote: get("deliveryNote"),
    isDefault: false, // default の表示は defaultOverride が担う
  }
}

/**
 * 配送先の一覧・追加・編集・削除・デフォルト変更を束ねるクライアントコンポーネント。
 *
 * 楽観的オーバーレイ（card-list と同じ思想）:
 * server action の成功 = イベント確定だが、 一覧の元になる ReadModel（projection）は非同期なので、
 * 直後の refresh が古い一覧を返すことがある。 操作結果はクライアントが確定的に知っているため、
 * ここで即座に上書き描画し、 サーバーは eventually 追いつく。
 */
export function ShippingAddressesManager({ initialAddresses }: { initialAddresses: ShippingAddress[] }) {
  const router = useRouter()
  const [isPending, startTransition] = useTransition()
  const [listError, setListError] = useState<string | null>(null)

  // 楽観的オーバーレイ
  const [added, setAdded] = useState<ShippingAddress[]>([])
  const [deletedIds, setDeletedIds] = useState<ReadonlySet<string>>(new Set())
  const [updatedOverrides, setUpdatedOverrides] = useState<ReadonlyMap<string, ShippingAddress>>(new Map())
  const [defaultOverride, setDefaultOverride] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)

  // サーバーが追いついたら added と重複しうるので id で重複排除（サーバー側を正とする）
  const base = [...initialAddresses, ...added.filter((a) => !initialAddresses.some((s) => s.id === a.id))]
  const displayAddresses = base
    .filter((address) => !deletedIds.has(address.id))
    .map((address) => updatedOverrides.get(address.id) ?? address)
    .map((address) => (defaultOverride ? { ...address, isDefault: address.id === defaultOverride } : address))

  function handleAdded(address: ShippingAddress, becomesDefault: boolean) {
    setAdded((prev) => [...prev, address])
    if (becomesDefault) setDefaultOverride(address.id)
    router.refresh()
  }

  function handleSaved(address: ShippingAddress) {
    setUpdatedOverrides((prev) => new Map(prev).set(address.id, address))
    setEditingId(null)
    router.refresh()
  }

  function handleSetDefault(id: string) {
    startTransition(async () => {
      setListError(null)
      const result = await changeDefaultShippingAddress(id)
      if (result?.error) {
        setListError(result.error)
        return
      }
      setDefaultOverride(id)
      router.refresh()
    })
  }

  function handleDelete(id: string) {
    startTransition(async () => {
      setListError(null)
      const result = await deleteShippingAddress(id)
      if (result?.error) {
        setListError(result.error)
        return
      }
      const deleted = displayAddresses.find((address) => address.id === id)
      const survivors = displayAddresses.filter((address) => address.id !== id)
      // backend は「default を消したら最古の残りを昇格」する（一覧は登録順なので先頭 = 最古）。 同じルールをミラー。
      if (deleted?.isDefault && survivors.length > 0) {
        setDefaultOverride(survivors[0].id)
      }
      setDeletedIds((prev) => new Set(prev).add(id))
      router.refresh()
    })
  }

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-medium">登録済みの配送先</h2>
        {displayAddresses.length === 0 ? (
          <p className="text-sm text-muted-foreground">登録済みの配送先はありません。</p>
        ) : (
          <ul className="flex flex-col gap-4">
            {displayAddresses.map((address) =>
              editingId === address.id ? (
                <li key={address.id} className="rounded-lg border p-4">
                  <EditAddressForm
                    address={address}
                    onSaved={handleSaved}
                    onCancel={() => setEditingId(null)}
                  />
                </li>
              ) : (
                <AddressCard
                  key={address.id}
                  address={address}
                  isPending={isPending}
                  onEdit={() => setEditingId(address.id)}
                  onSetDefault={() => handleSetDefault(address.id)}
                  onDelete={() => handleDelete(address.id)}
                />
              ),
            )}
          </ul>
        )}
        {listError && <p className="text-sm text-destructive">{listError}</p>}
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-medium">配送先を追加</h2>
        <AddAddressForm hasAddresses={displayAddresses.length > 0} onAdded={handleAdded} />
      </section>
    </div>
  )
}

function AddressCard({
  address,
  isPending,
  onEdit,
  onSetDefault,
  onDelete,
}: {
  address: ShippingAddress
  isPending: boolean
  onEdit: () => void
  onSetDefault: () => void
  onDelete: () => void
}) {
  return (
    <li
      className={`flex flex-col gap-2 rounded-lg border p-4 ${
        address.isDefault ? "border-primary ring-1 ring-primary" : ""
      }`}
    >
      <div className="flex items-center gap-2">
        <span className="font-medium">{address.name}</span>
        {address.isDefault && (
          <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs text-primary">デフォルト</span>
        )}
      </div>
      <p className="text-sm">
        〒{address.postalCode} {address.prefecture}
        {address.city}
        {address.streetAddress}
        {address.building && ` ${address.building}`}
      </p>
      <p className="text-sm text-muted-foreground">📞 {address.phoneNumber}</p>
      {address.deliveryNote && <p className="text-sm text-muted-foreground">📝 {address.deliveryNote}</p>}
      <div className="flex items-center justify-end gap-2">
        <Button type="button" variant="outline" size="sm" onClick={onEdit} disabled={isPending}>
          編集
        </Button>
        {!address.isDefault && (
          <Button type="button" variant="outline" size="sm" onClick={onSetDefault} disabled={isPending}>
            デフォルトにする
          </Button>
        )}
        <Button type="button" variant="destructive" size="sm" onClick={onDelete} disabled={isPending}>
          削除
        </Button>
      </div>
    </li>
  )
}

function AddAddressForm({
  hasAddresses,
  onAdded,
}: {
  hasAddresses: boolean
  onAdded: (address: ShippingAddress, becomesDefault: boolean) => void
}) {
  const [state, setState] = useState<SaveAddressState>(null)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = e.currentTarget
    const formData = new FormData(form)
    setSubmitting(true)
    const result = await registerShippingAddress(formData)
    setSubmitting(false)
    setState(result)
    if (!result?.success || !result.id) return
    const makeDefault = formData.get("makeDefault") === "on"
    // 初回登録は backend が自動で default にする。 そのルールをミラーして即時反映。
    onAdded(addressFromForm(result.id, formData), !hasAddresses || makeDefault)
    form.reset()
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      <AddressFormFields idPrefix="add-" fieldErrors={state?.fieldErrors} />

      {hasAddresses && (
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" name="makeDefault" className="size-4" />
          この配送先をデフォルトにする
        </label>
      )}

      {state?.error && <p className="text-sm text-destructive">{state.error}</p>}

      <Button type="submit" disabled={submitting} className="w-fit">
        {submitting ? "登録中..." : "配送先を登録"}
      </Button>
    </form>
  )
}

function EditAddressForm({
  address,
  onSaved,
  onCancel,
}: {
  address: ShippingAddress
  onSaved: (address: ShippingAddress) => void
  onCancel: () => void
}) {
  const [state, setState] = useState<SaveAddressState>(null)
  const [submitting, setSubmitting] = useState(false)

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    setSubmitting(true)
    const result = await updateShippingAddress(address.id, formData)
    setSubmitting(false)
    setState(result)
    if (!result?.success) return
    // isDefault は編集で変わらない（changedefault の責務）ので現在値を引き継ぐ
    onSaved({ ...addressFromForm(address.id, formData), isDefault: address.isDefault })
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      <AddressFormFields idPrefix={`edit-${address.id}-`} defaults={address} fieldErrors={state?.fieldErrors} />

      {state?.error && <p className="text-sm text-destructive">{state.error}</p>}

      <div className="flex gap-2">
        <Button type="submit" disabled={submitting}>
          {submitting ? "保存中..." : "保存"}
        </Button>
        <Button type="button" variant="outline" onClick={onCancel} disabled={submitting}>
          キャンセル
        </Button>
      </div>
    </form>
  )
}
