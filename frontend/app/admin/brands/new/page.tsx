import Link from "next/link";
import { BrandCreateForm } from "./brand-create-form";

export default function NewBrandPage() {
  return (
    <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">ブランド新規作成</h1>
        <Link
          href="/admin/brands"
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          戻る
        </Link>
      </div>

      <BrandCreateForm />
    </main>
  );
}
