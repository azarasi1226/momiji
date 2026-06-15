import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { brandStatusLabel } from "@/lib/status-labels";
import { fetchBrand } from "../actions";
import { ArchiveBrandButton } from "./archive-brand-button";
import { BrandEditForm } from "./brand-edit-form";

export default async function BrandDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const brand = await fetchBrand(id);

  return (
    <main className="flex w-full max-w-2xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">ブランド編集</h1>
        <Link
          href="/admin/brands"
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          戻る
        </Link>
      </div>

      <div className="flex items-center gap-3 text-xs text-muted-foreground">
        <span>ID: {brand.id}</span>
        <Badge variant={brand.status === "ARCHIVED" ? "secondary" : "default"}>
          {brandStatusLabel(brand.status)}
        </Badge>
      </div>

      <BrandEditForm brand={brand} />

      <Separator />

      <ArchiveBrandButton id={brand.id} />
    </main>
  );
}
