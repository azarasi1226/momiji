import NextAuth from "next-auth"
import Keycloak from "next-auth/providers/keycloak"

export const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:9090"

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [Keycloak],
  callbacks: {
    async signIn({ account }) {
      if (!account?.access_token) {
        return false
      }

      try {
        const res = await fetch(`${BACKEND_URL}/users/me`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${account.access_token}`,
          },
        })

        if (!res.ok) {
          console.error("CreateUser API failed:", res.status, await res.text())
          return false
        }

        return true
      } catch (e) {
        console.error("CreateUser API call error:", e)
        return false
      }
    },
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token
        token.refreshToken = account.refresh_token
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
      session.error = token.error
      return session
    },
  },
})
