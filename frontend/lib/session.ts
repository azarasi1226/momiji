import { redirect } from "next/navigation";
import { auth } from "@/auth";

/**
 * Server Action / Server Component の冒頭で呼ぶ。
 * 有効な session が無い場合は "/" にリダイレクトする。
 *
 * リダイレクト条件:
 * - そもそも未ログイン (session 無し / accessToken 無し)
 * - refresh token rotation が失敗した (session.error === "RefreshTokenError")
 *
 * これを 1 か所に集約することで、 各 action の冒頭ボイラープレートを削減する。
 */
export async function requireValidSession() {
  const session = await auth();
  if (!session?.accessToken || session.error === "RefreshTokenError") {
    redirect("/");
  }
  return session;
}
