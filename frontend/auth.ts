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
      }
      return token
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken as string
      return session
    },
  },
})
