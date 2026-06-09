"use client"

import { useActionState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
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
      <Card className="py-0">
        <CardContent className="px-4 py-4">
          <form action={receiveAction} className="flex flex-col gap-3">
            <input type="hidden" name="productId" value={productId} />
            <h3 className="text-sm font-medium">入庫</h3>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="receive-quantity" className="text-xs text-muted-foreground">
                入庫数
              </Label>
              <Input
                id="receive-quantity"
                name="quantity"
                type="number"
                min={1}
                defaultValue={1}
                required
                aria-invalid={!!receiveState?.fieldErrors?.quantity}
              />
            </div>
            {receiveState?.error && (
              <p className="text-xs text-destructive">{receiveState.error}</p>
            )}
            {receiveState?.fieldErrors?.quantity && (
              <p className="text-xs text-destructive">{receiveState.fieldErrors.quantity}</p>
            )}
            {receiveState?.success && <p className="text-xs text-green-600">入庫しました</p>}
            <Button type="submit" disabled={receivePending}>
              {receivePending ? "処理中..." : "入庫する"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {/* 調整 */}
      <Card className="py-0">
        <CardContent className="px-4 py-4">
          <form action={adjustAction} className="flex flex-col gap-3">
            <input type="hidden" name="productId" value={productId} />
            <h3 className="text-sm font-medium">在庫調整</h3>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="adjust-quantity" className="text-xs text-muted-foreground">
                調整数（増加は +、 減少は −）
              </Label>
              <Input
                id="adjust-quantity"
                name="quantity"
                type="number"
                defaultValue={-1}
                required
                aria-invalid={!!adjustState?.fieldErrors?.quantity}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="adjust-reason" className="text-xs text-muted-foreground">
                理由
              </Label>
              <Select name="reason" defaultValue="DAMAGED">
                <SelectTrigger id="adjust-reason">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {REASON_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            {/* 増加できるのは棚卸しのときだけ（サーバが検証する）。 */}
            <p className="text-[11px] text-muted-foreground">
              在庫を増やす調整ができるのは「棚卸し差異」のときだけです。
            </p>
            {adjustState?.error && <p className="text-xs text-destructive">{adjustState.error}</p>}
            {adjustState?.fieldErrors?.quantity && (
              <p className="text-xs text-destructive">{adjustState.fieldErrors.quantity}</p>
            )}
            {adjustState?.fieldErrors?.reason && (
              <p className="text-xs text-destructive">{adjustState.fieldErrors.reason}</p>
            )}
            {adjustState?.success && <p className="text-xs text-green-600">調整しました</p>}
            <Button type="submit" variant="outline" disabled={adjustPending}>
              {adjustPending ? "処理中..." : "調整する"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
