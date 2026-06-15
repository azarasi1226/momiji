import NextAuth from "next-auth";
import { CreateUserService } from "@/grpc/gen/momiji/user/create/v1/create_pb.js";
import { createGrpcClient } from "@/lib/grpc";
import { formatConnectError } from "@/lib/grpc-error";
import { activeProvider, refreshAccessToken } from "@/lib/idp";

export const { handlers, signIn, signOut, auth } = NextAuth({
  // provider は AUTH_PROVIDER で keycloak / cognito を切り替える (lib/idp.ts)。
  providers: [activeProvider],
  callbacks: {
    async signIn({ account }) {
      if (!account?.access_token) {
        return "/auth/error?reason=no_token";
      }

      try {
        const client = createGrpcClient(
          CreateUserService,
          account.access_token,
        );
        await client.createUser({});
        return true;
      } catch (e) {
        console.error("CreateUser gRPC call error:", e);
        // gRPC エラーの中身 (code / メッセージ / correlationId) をエラーページへ渡して可視化する。
        const { code, message, correlationId } = formatConnectError(e);
        const params = new URLSearchParams({ reason: "backend" });
        if (code) params.set("code", code);
        if (message) params.set("message", message);
        if (correlationId) params.set("correlationId", correlationId);
        return `/auth/error?${params.toString()}`;
      }
    },
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.idToken = account.id_token;
        token.expiresAt = account.expires_at ?? 0;
      }

      // アクセストークンがまだ有効ならそのまま返す
      if (Date.now() < (token.expiresAt as number) * 1000) {
        return token;
      }

      // 期限切れならリフレッシュ (token endpoint は provider に応じて discovery で解決される)
      if (!token.refreshToken) {
        return { ...token, error: "RefreshTokenError" };
      }
      try {
        const result = await refreshAccessToken(token.refreshToken as string);
        if (!result.ok) {
          // status code も一緒に出すことで invalid_grant (refresh token 期限切れ) と
          // 設定不整合 (invalid_client 等) を切り分けやすくする。
          console.error(
            `Token refresh failed (status ${result.status}):`,
            result.data,
          );
          return { ...token, error: "RefreshTokenError" };
        }

        return {
          ...token,
          accessToken: result.data.access_token,
          refreshToken: result.data.refresh_token ?? token.refreshToken,
          expiresAt: Math.floor(Date.now() / 1000) + result.data.expires_in,
        };
      } catch (e) {
        console.error("Token refresh error:", e);
        return { ...token, error: "RefreshTokenError" };
      }
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string;
      session.idToken = token.idToken as string;
      session.error = token.error;
      return session;
    },
  },
});
