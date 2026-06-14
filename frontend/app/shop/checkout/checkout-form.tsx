"use client"

import { useState, useTransition } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { ShippingAddress } from "@/app/profile/shipping-addresses/actions"
import { startOrder } from "./actions"

export type CheckoutItem = {
  productId: string
  productName: string
  productPrice: number
  itemQuantity: number
}

type Props = {
  items: CheckoutItem[]
  total: number
  addresses: ShippingAddress[]
}

function addressLabel(a: ShippingAddress): string {
  return `${a.name} 様 / ${a.prefecture}${a.city}${a.streetAddress}${a.building ? ` ${a.building}` : ""}`
}

/** 注文手続き: 配送先を選び、 注文内容・合計を確認して注文を開始する。 */
export function CheckoutForm({ items, total, addresses }: Props) {
  const defaultAddress = addresses.find((a) => a.isDefault) ?? addresses[0]
  const [addressId, setAddressId] = useState(defaultAddress?.id ?? "")
  const [isPending, startTransition] = useTransition()
  const [error, setError] = useState<string | null>(null)
  const [orderId, setOrderId] = useState<string | null>(null)

  function handleOrder() {
    setError(null)
    startTransition(async () => {
      const res = await startOrder(addressId, total)
      if (res?.error) {
        setError(res.error)
        return
      }
      if (res?.success) {
        setOrderId(res.orderId ?? null)
      }
    })
  }

  // 注文開始成功。 決済（決済準備）はこの先のステップ（未実装）。
  if (orderId) {
    return (
      <Card className="flex flex-col items-start gap-3 p-6">
        <p className="text-lg font-semibold">注文を開始しました</p>
        <p className="text-sm text-muted-foreground">
          在庫を確保しました。 注文番号: <span className="font-mono">{orderId}</span>
        </p>
        <p className="text-sm text-muted-foreground">
          ※ お支払い手続きはこの先のステップです（現在準備中）。
        </p>
        <Button asChild variant="outline" size="sm">
          <Link href="/shop/products">買い物を続ける</Link>
        </Button>
      </Card>
    )
  }

  // 配送先が未登録なら、 先に登録してもらう。
  if (addresses.length === 0) {
    return (
      <Card className="flex flex-col items-start gap-3 p-6">
        <p className="text-sm text-muted-foreground">
          配送先が登録されていません。 先に配送先を登録してください。
        </p>
        <Button asChild size="sm">
          <Link href="/profile/shipping-addresses">配送先を登録する</Link>
        </Button>
      </Card>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">お届け先</h2>
        <Select value={addressId} onValueChange={setAddressId}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="配送先を選択" />
          </SelectTrigger>
          <SelectContent>
            {addresses.map((a) => (
              <SelectItem key={a.id} value={a.id}>
                {addressLabel(a)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Link
          href="/profile/shipping-addresses"
          className="text-xs text-muted-foreground transition-colors hover:text-foreground"
        >
          配送先を追加・編集する
        </Link>
      </section>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-medium">注文内容</h2>
        <Card className="px-5 py-3">
          {items.map((item) => (
            <div
              key={item.productId}
              className="flex items-center justify-between py-2 text-sm"
            >
              <span>
                {item.productName}
                <span className="text-muted-foreground"> × {item.itemQuantity}</span>
              </span>
              <span>
                ¥{(item.productPrice * item.itemQuantity).toLocaleString("ja-JP")}
              </span>
            </div>
          ))}
        </Card>
      </section>

      <Separator />

      <div className="flex items-center justify-between">
        <span className="text-sm text-muted-foreground">合計</span>
        <span className="text-lg font-semibold">¥{total.toLocaleString("ja-JP")}</span>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <Button onClick={handleOrder} disabled={isPending || !addressId} size="lg">
        {isPending ? "処理中..." : "注文する"}
      </Button>

      <Link
        href="/shop/basket"
        className="text-sm text-muted-foreground transition-colors hover:text-foreground"
      >
        ← 買い物かごに戻る
      </Link>
    </div>
  )
}
