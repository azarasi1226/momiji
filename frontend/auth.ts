import NextAuth from "next-auth"
import Keycloak from "next-auth/providers/keycloak"
import { createGrpcClient } from "@/lib/grpc"
import { CreateUserService } from "@/grpc/gen/momiji/user/create/v1/create_pb.js"

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [Keycloak],
  callbacks: {
    async signIn({ account }) {
      if (!account?.access_token) {
        return "/auth/error?reason=no_token"
      }

      try {
        const client = createGrpcClient(CreateUserService, account.access_token)
        await client.createUser({})
        return true
      } catch (e) {
        console.error("CreateUser gRPC call error:", e)
        return "/auth/error?reason=backend"
      }
    },
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token
        token.refreshToken = account.refresh_token
        token.idToken = account.id_token
        token.expiresAt = account.expires_at ?? 0
      }

      // アクセストークンがまだ有効ならそのまま返す
      if (Date.now() < (token.expiresAt as number) * 1000) {
        return token
      }

      // 期限切れならリフレッシュ
      try {
        const issuer = process.env.AUTH_KEYCLOAK_ISSUER!
        const res = await fetch(`${issuer}/protocol/openid-connect/token`, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({
            client_id: process.env.AUTH_KEYCLOAK_ID!,
            client_secret: process.env.AUTH_KEYCLOAK_SECRET!,
            grant_type: "refresh_token",
            refresh_token: token.refreshToken as string,
          }),
        })

        const data = await res.json()

        if (!res.ok) {
          console.error("Token refresh failed:", data)
          return { ...token, error: "RefreshTokenError" }
        }

        return {
          ...token,
          accessToken: data.access_token,
          refreshToken: data.refresh_token ?? token.refreshToken,
          expiresAt: Math.floor(Date.now() / 1000) + data.expires_in,
        }
      } catch (e) {
        console.error("Token refresh error:", e)
        return { ...token, error: "RefreshTokenError" }
      }
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string
      session.idToken = token.idToken as string
      session.error = token.error
      return session
    },
  },
  events: {
    async signOut(message) {
      const token = "token" in message ? message.token : undefined
      if (token?.idToken) {
        const issuer = process.env.AUTH_KEYCLOAK_ISSUER!
        const logoutUrl = new URL(`${issuer}/protocol/openid-connect/logout`)
        logoutUrl.searchParams.set("id_token_hint", token.idToken as string)
        logoutUrl.searchParams.set("post_logout_redirect_uri", process.env.NEXTAUTH_URL ?? "http://localhost:3000")
        await fetch(logoutUrl.toString())
      }
    },
  },
})
