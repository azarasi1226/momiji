import type { Metadata } from "next";
import Link from "next/link";
import { Pagination } from "@/components/pagination";
import { QueryParamSelect } from "@/components/query-param-select";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { formatDateTime } from "@/lib/format";
import { productStatusLabel } from "@/lib/status-labels";
import { listAllBrands } from "../brands/queries";
import { listProducts } from "./queries";

export const metadata: Metadata = {
  title: "商品管理",
};

const PAGE_SIZE = 20;

const SORT_OPTIONS = [
  { value: "name_asc", label: "名前 昇順" },
  { value: "name_desc", label: "名前 降順" },
  { value: "price_asc", label: "価格 安い順" },
  { value: "price_desc", label: "価格 高い順" },
  { value: "created_desc", label: "新しい順" },
  { value: "created_asc", label: "古い順" },
];

// Radix Select は空文字値を許さないので「すべて」は "all" センチネルにし、サーバ側で "" に正規化する。
const ALL = "all";

const STATUS_OPTIONS = [
  { value: ALL, label: "すべて" },
  { value: "ACTIVE", label: "販売中" },
  { value: "DISCONTINUED", label: "生産終了" },
];

export default async function ProductListPage({
  searchParams,
}: {
  searchParams: Promise<{
    q?: string;
    status?: string;
    brand?: string;
    sort?: string;
    page?: string;
  }>;
}) {
  const sp = await searchParams;
  const likeName = sp.q ?? "";
  const statusParam = sp.status ?? ALL;
  const brandParam = sp.brand ?? ALL;
  const status = statusParam === ALL ? "" : statusParam;
  const brandId = brandParam === ALL ? "" : brandParam;
  const sort = sp.sort ?? "name_asc";
  const pageNumber = Math.max(1, Number(sp.page ?? "1") || 1);

  const [page, brands] = await Promise.all([
    listProducts({
      likeName,
      status,
      brandId,
      sort,
      pageSize: PAGE_SIZE,
      pageNumber,
    }),
    listAllBrands(),
  ]);

  const brandNames = Object.fromEntries(brands.map((b) => [b.id, b.name]));
  const brandOptions = [
    { value: ALL, label: "すべて" },
    ...brands.map((b) => ({ value: b.id, label: b.name })),
  ];

  return (
    <main className="flex w-full max-w-5xl flex-col gap-6 px-8 py-16">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">商品管理</h1>
        <Button asChild>
          <Link href="/admin/products/new">新規作成</Link>
        </Button>
      </div>

      {/* 左: 絞り込み（商品名・状態・ブランド）を検索ボタンで適用。 右: 並び順（変更で即適用）。 */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <form method="get" className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="q" className="text-xs text-muted-foreground">
              商品名で検索
            </Label>
            <Input
              id="q"
              name="q"
              type="text"
              defaultValue={likeName}
              placeholder="部分一致"
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="status" className="text-xs text-muted-foreground">
              状態
            </Label>
            <Select name="status" defaultValue={statusParam}>
              <SelectTrigger id="status" className="w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="brand" className="text-xs text-muted-foreground">
              ブランド
            </Label>
            <Select name="brand" defaultValue={brandParam}>
              <SelectTrigger id="brand" className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {brandOptions.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {/* 検索時に現在の並び順を維持する */}
          <input type="hidden" name="sort" value={sort} />
          <Button type="submit" variant="outline">
            検索
          </Button>
        </form>

        <QueryParamSelect
          param="sort"
          value={sort}
          label="並び順"
          options={SORT_OPTIONS}
        />
      </div>

      {page.products.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          条件に一致する商品がありません。
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>商品名</TableHead>
              <TableHead>ブランド</TableHead>
              <TableHead>価格</TableHead>
              <TableHead>在庫</TableHead>
              <TableHead>状態</TableHead>
              <TableHead>更新日時</TableHead>
              <TableHead className="text-right" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {page.products.map((product) => (
              <TableRow key={product.id}>
                <TableCell className="font-medium">{product.name}</TableCell>
                <TableCell className="text-muted-foreground">
                  {brandNames[product.brandId] ?? product.brandId}
                </TableCell>
                <TableCell>¥{product.price.toLocaleString("ja-JP")}</TableCell>
                <TableCell>
                  <span
                    className={
                      product.stockAvailable <= 0 ? "text-destructive" : ""
                    }
                  >
                    {product.stockAvailable.toLocaleString("ja-JP")}
                  </span>
                  {product.stockReserved > 0 && (
                    <span className="text-xs text-muted-foreground">
                      {" "}
                      （確保 {product.stockReserved.toLocaleString("ja-JP")}）
                    </span>
                  )}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={
                      product.status === "DISCONTINUED"
                        ? "secondary"
                        : "default"
                    }
                  >
                    {productStatusLabel(product.status)}
                  </Badge>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {formatDateTime(product.updatedAt)}
                </TableCell>
                <TableCell className="text-right">
                  <Button asChild variant="link" size="sm">
                    <Link href={`/admin/products/${product.id}`}>編集</Link>
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <div className="flex flex-col items-center gap-3">
        <Pagination currentPage={page.pageNumber} totalPage={page.totalPage} />
        <p className="text-xs text-muted-foreground">
          全 {page.totalCount.toLocaleString("ja-JP")} 件
        </p>
      </div>
    </main>
  );
}
