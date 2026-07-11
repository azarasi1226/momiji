import Link from "next/link";
import { redirect } from "next/navigation";
import { auth, signIn, signOut } from "@/auth";
import { Button } from "@/components/ui/button";
import { buildLogoutUrl } from "@/lib/idp";

export default async function Home() {
  const session = await auth();
  const sessionExpired = session?.error === "RefreshTokenError";
  // 期限切れ session は「ログイン中」 として扱わない。 そうしないと profile への遷移で
  // 「/profile → / リダイレクト → / でログイン中 UI 表示」 のループにハマる。
  const isLoggedIn = !!session && !sessionExpired;

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 font-sans">
      <main className="flex min-h-screen w-full max-w-3xl flex-col items-center justify-center gap-8 bg-background px-16 py-32">
        {isLoggedIn ? (
          <>
            <h1 className="text-2xl font-semibold">ログイン中</h1>
            <div className="text-center text-muted-foreground">
              <p>{session.user?.name}</p>
              <p>{session.user?.email}</p>
            </div>
            <div className="flex flex-wrap justify-center gap-3">
              <Button asChild size="lg">
                <Link href="/shop/products">商品一覧</Link>
              </Button>
              <Button asChild variant="outline" size="lg">
                <Link href="/profile">プロフィール</Link>
              </Button>
              <Button asChild variant="outline" size="lg">
                <Link href="/admin/brands">ブランド管理</Link>
              </Button>
              <form
                action={async () => {
                  "use server";
                  // 1. アプリ側 session を消す。signOut({ redirectTo: logoutUrl }) で IdP の logout へ
                  //    直接飛ばせないのは、Auth.js の既定 redirect callback が baseUrl とオリジンの
                  //    違う URL を baseUrl に丸めてしまうため (open-redirect 対策。@auth/core の
                  //    createCallbackUrl → callbacks.redirect)。IdP の logout は別オリジンなので丸められる。
                  //    よって redirect:false で破棄だけ行い、外部オリジンへ飛ばせる next/navigation の
                  //    redirect() を 2 で使う。
                  const current = await auth();
                  await signOut({ redirect: false });
                  // 2. ブラウザを IdP の logout エンドポイントへ飛ばし、 IdP 側のログインセッションも破棄する。
                  //    これをやらないと Cognito/Keycloak の session cookie が残り、 再ログインが素通りしてしまう。
                  const logoutUrl = await buildLogoutUrl({
                    idToken: current?.idToken,
                    postLogoutRedirectUri:
                      process.env.AUTH_URL ?? "http://localhost:4000",
                  });
                  redirect(logoutUrl);
                }}
              >
                <Button type="submit" variant="outline" size="lg">
                  ログアウト
                </Button>
              </form>
            </div>
          </>
        ) : (
          <>
            <h1 className="text-2xl font-semibold">momiji</h1>
            {sessionExpired && (
              <p className="text-sm text-destructive">
                セッションの有効期限が切れました。 再度ログインしてください。
              </p>
            )}
            <form
              action={async () => {
                "use server";
                // 期限切れ session には古い token が残っているので、 先にクリアしてから signIn する。
                // そうしないと NextAuth が古い jwt を引き続き使おうとして再ログインが効かないことがある。
                if (sessionExpired) {
                  await signOut({ redirect: false });
                }
                await signIn("oidc");
              }}
            >
              <Button type="submit" size="lg">
                ログイン
              </Button>
            </form>
          </>
        )}
      </main>
    </div>
  );
}
