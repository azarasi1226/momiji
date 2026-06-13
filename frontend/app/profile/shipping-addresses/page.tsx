import Link from "next/link"
import { fetchShippingAddresses } from "./actions"
import { ShippingAddressesManager } from "./shipping-addresses-manager"

export default async function ShippingAddressesPage() {
  const addresses = await fetchShippingAddresses()

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-8 p-6">
      <div className="flex items-start justify-between">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold">配送先</h1>
          <p className="text-sm text-muted-foreground">お届け先の住所を管理します。</p>
        </div>
        <Link
          href="/profile"
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          プロフィールへ戻る
        </Link>
      </div>

      <ShippingAddressesManager initialAddresses={addresses} />
    </div>
  )
}
