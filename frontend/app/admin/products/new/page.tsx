import Link from "next/link"
import { listActiveBrands } from "../actions"
import { ProductCreateForm } from "./product-create-form"

export default async function NewProductPage() {
  const brands = await listActiveBrands()

  return (
    <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">商品新規作成</h1>
        <Link
          href="/admin/products"
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          戻る
        </Link>
      </div>

      <ProductCreateForm brands={brands} />
    </main>
  )
}
