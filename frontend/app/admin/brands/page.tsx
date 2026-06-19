import type { Metadata } from "next";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { formatDateTime } from "@/lib/format";
import { brandStatusLabel } from "@/lib/status-labels";
import { listBrands } from "./queries";

export const metadata: Metadata = {
  title: "ブランド管理",
};

export default async function BrandListPage() {
  const brands = await listBrands();

  return (
    <main className="flex w-full max-w-4xl flex-col gap-8 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">ブランド管理</h1>
        <Button asChild>
          <Link href="/admin/brands/new">新規作成</Link>
        </Button>
      </div>

      {brands.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          ブランドがまだ登録されていません。
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ブランド名</TableHead>
              <TableHead>説明</TableHead>
              <TableHead>状態</TableHead>
              <TableHead>更新日時</TableHead>
              <TableHead className="text-right" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {brands.map((brand) => (
              <TableRow key={brand.id}>
                <TableCell className="font-medium">{brand.name}</TableCell>
                <TableCell className="max-w-xs truncate text-muted-foreground">
                  {brand.description || "—"}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={
                      brand.status === "ARCHIVED" ? "secondary" : "default"
                    }
                  >
                    {brandStatusLabel(brand.status)}
                  </Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {formatDateTime(brand.updatedAt)}
                </TableCell>
                <TableCell className="text-right">
                  <Button asChild variant="link" size="sm">
                    <Link href={`/admin/brands/${brand.id}`}>編集</Link>
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </main>
  );
}
