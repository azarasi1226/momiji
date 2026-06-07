"use client"

import { useActionState } from "react"
import { adjustStock, receiveStock, type ProductFormState } from "../actions"

const REASON_OPTIONS = [
  { value: "DAMAGED", label: "破損・廃棄" },
  { value: "LOST", label: "紛失・盗難" },
  { value: "STOCKTAKING", label: "棚卸し差異" },
  { value: "CORRECTION", label: "訂正" },
  { value: "OTHER", label: "その他" },
]

export function StockForms({ productId }: { productId: string }) {
  const [receiveState, receiveAction, receivePending] = useActionState<
    ProductFormState,
    FormData
  >(receiveStock, null)
  const [adjustState, adjustAction, adjustPending] = useActionState<
    ProductFormState,
    FormData
  >(adjustStock, null)

  return (
    <div className="grid gap-6 sm:grid-cols-2">
      {/* 入庫 */}
      <form
        action={receiveAction}
        className="flex flex-col gap-3 rounded-2xl border border-zinc-200 p-4 dark:border-zinc-800"
      >
        <input type="hidden" name="productId" value={productId} />
        <h3 className="text-sm font-medium text-black dark:text-zinc-50">入庫</h3>
        <div className="flex flex-col gap-1">
          <label htmlFor="receive-quantity" className="text-xs text-zinc-500 dark:text-zinc-400">
            入庫数
          </label>
          <input
            id="receive-quantity"
            name="quantity"
            type="number"
            min={1}
            defaultValue={1}
            required
            className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
          />
        </div>
        {receiveState?.error && (
          <p className="text-xs text-red-500 dark:text-red-400">{receiveState.error}</p>
        )}
        {receiveState?.fieldErrors?.quantity && (
          <p className="text-xs text-red-500 dark:text-red-400">
            {receiveState.fieldErrors.quantity}
          </p>
        )}
        {receiveState?.success && (
          <p className="text-xs text-green-600 dark:text-green-400">入庫しました</p>
        )}
        <button
          type="submit"
          disabled={receivePending}
          className="mt-auto flex h-10 items-center justify-center rounded-full bg-foreground px-6 text-sm text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
        >
          {receivePending ? "処理中..." : "入庫する"}
        </button>
      </form>

      {/* 調整 */}
      <form
        action={adjustAction}
        className="flex flex-col gap-3 rounded-2xl border border-zinc-200 p-4 dark:border-zinc-800"
      >
        <input type="hidden" name="productId" value={productId} />
        <h3 className="text-sm font-medium text-black dark:text-zinc-50">在庫調整</h3>
        <div className="flex flex-col gap-1">
          <label htmlFor="adjust-quantity" className="text-xs text-zinc-500 dark:text-zinc-400">
            調整数（増加は +、 減少は −）
          </label>
          <input
            id="adjust-quantity"
            name="quantity"
            type="number"
            defaultValue={-1}
            required
            className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
          />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor="adjust-reason" className="text-xs text-zinc-500 dark:text-zinc-400">
            理由
          </label>
          <select
            id="adjust-reason"
            name="reason"
            defaultValue="DAMAGED"
            className="h-10 rounded-lg border border-zinc-200 px-3 text-sm dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
          >
            {REASON_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        {/* 増加できるのは棚卸しのときだけ（サーバが検証する）。 */}
        <p className="text-[11px] text-zinc-400 dark:text-zinc-500">
          在庫を増やす調整ができるのは「棚卸し差異」のときだけです。
        </p>
        {adjustState?.error && (
          <p className="text-xs text-red-500 dark:text-red-400">{adjustState.error}</p>
        )}
        {adjustState?.fieldErrors?.quantity && (
          <p className="text-xs text-red-500 dark:text-red-400">
            {adjustState.fieldErrors.quantity}
          </p>
        )}
        {adjustState?.fieldErrors?.reason && (
          <p className="text-xs text-red-500 dark:text-red-400">
            {adjustState.fieldErrors.reason}
          </p>
        )}
        {adjustState?.success && (
          <p className="text-xs text-green-600 dark:text-green-400">調整しました</p>
        )}
        <button
          type="submit"
          disabled={adjustPending}
          className="mt-auto flex h-10 items-center justify-center rounded-full border border-zinc-200 px-6 text-sm text-zinc-700 transition-colors hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:text-zinc-200 dark:hover:bg-zinc-900"
        >
          {adjustPending ? "処理中..." : "調整する"}
        </button>
      </form>
    </div>
  )
}
