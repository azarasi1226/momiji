import { Package, ShoppingCart } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";

export const metadata: Metadata = {
  title: {
    default: "ショップ",
    template: "%s | momiji shop",
  },
};

/**
 * /shop/* 共通シェル（購入者向け）。 上部ヘッダー（商品一覧 / カゴ）+ ページ内容。
 *
 * 認証ゲートをここに集約する: カゴは JWT から持ち主を解決するためログイン必須。
 * layout は配下ページの親として先に実行され、redirect すればページは描画されない。
 */
export default async function ShopLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const session = await auth();
  if (!session || session.error === "RefreshTokenError") {
    redirect("/");
  }

  const name = session.user?.name;
  const email = session.user?.email;
  const image = session.user?.image;
  const label = name || email || "プロフィール";
  const initial = (name || email || "?").charAt(0).toUpperCase();

  return (
    <div className="flex min-h-screen flex-col bg-muted/30 font-sans">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b bg-background/80 px-8 py-4 backdrop-blur">
        <div className="flex items-center gap-6">
          <Link href="/shop/products" className="text-lg font-semibold">
            momiji shop
          </Link>
          <Link
            href="/shop/products"
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            商品一覧
          </Link>
        </div>
        <div className="flex items-center gap-3">
          <Button asChild variant="outline">
            <Link href="/shop/orders">
              <Package />
              注文履歴
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link href="/shop/basket">
              <ShoppingCart />
              買い物かご
            </Link>
          </Button>
          <Link href="/profile" aria-label="プロフィール" title={label}>
            <Avatar>
              {image ? <AvatarImage src={image} alt={label} /> : null}
              <AvatarFallback>{initial}</AvatarFallback>
            </Avatar>
          </Link>
        </div>
      </header>
      <div className="flex flex-1 justify-center">{children}</div>
    </div>
  );
}
