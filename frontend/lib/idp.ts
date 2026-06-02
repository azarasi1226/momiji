import Keycloak from "next-auth/providers/keycloak"
import Cognito from "next-auth/providers/cognito"

// AUTH_PROVIDER で local(Keycloak) / prod(Cognito) を環境ごとに切り替える (ADR 0003 の 2 IDP 運用)。
// 各 provider の client_id / secret / issuer は Auth.js 規約の AUTH_<PROVIDER>_* env から読む。
type ProviderKey = "keycloak" | "cognito"

// provider に固定 id "oidc" を与えることで、 IDP を切り替えても callback URL を
// /api/auth/callback/oidc の 1 本に統一する (default だと /callback/keycloak と /callback/cognito に
// 分かれ、 IDP 特化の redirect URL になってしまう)。 id を上書きすると env 自動推論が効かないため、
// clientId / secret / issuer は明示的に渡す。
const SHARED_ID = "oidc"

const REGISTRY = {
  keycloak: {
    provider: Keycloak({
      id: SHARED_ID,
      name: "Keycloak",
      clientId: process.env.AUTH_KEYCLOAK_ID,
      clientSecret: process.env.AUTH_KEYCLOAK_SECRET,
      issuer: process.env.AUTH_KEYCLOAK_ISSUER,
    }),
    clientId: process.env.AUTH_KEYCLOAK_ID,
    clientSecret: process.env.AUTH_KEYCLOAK_SECRET,
    issuer: process.env.AUTH_KEYCLOAK_ISSUER,
  },
  cognito: {
    provider: Cognito({
      id: SHARED_ID,
      name: "Cognito",
      clientId: process.env.AUTH_COGNITO_ID,
      clientSecret: process.env.AUTH_COGNITO_SECRET,
      issuer: process.env.AUTH_COGNITO_ISSUER,
      // Cognito はソーシャル IdP ブローカリング時に id_token へ nonce を必ず入れてくる。
      // Auth.js のデフォルト checks は ["pkce"] で nonce を送らないため、 検証側が
      // expectNoNonce となり「unexpected nonce」で落ちる。 nonce を有効化して Auth.js からも
      // nonce を送り、 Cognito にエコーさせることで一致させる。
      checks: ["pkce", "nonce"],
    }),
    clientId: process.env.AUTH_COGNITO_ID,
    clientSecret: process.env.AUTH_COGNITO_SECRET,
    issuer: process.env.AUTH_COGNITO_ISSUER,
  },
} as const

const PROVIDER = process.env.AUTH_PROVIDER as ProviderKey | undefined

// 未設定/不正は prod での provider 取り違えにつながるため、 default を持たせず起動時に loud に落とす。
function resolveActive() {
  const a = PROVIDER ? REGISTRY[PROVIDER] : undefined
  if (!a) {
    throw new Error(
      `AUTH_PROVIDER を "keycloak" か "cognito" で設定してください (現在: "${PROVIDER ?? "未設定"}")`,
    )
  }
  if (!a.issuer) {
    throw new Error(`${PROVIDER} の issuer env (AUTH_${PROVIDER!.toUpperCase()}_ISSUER) が未設定です`)
  }
  return a
}

const active = resolveActive()

/** 現在の環境で有効な Auth.js provider。 id を "oidc" 固定にしているので callback URL は常に /api/auth/callback/oidc。 */
export const activeProvider = active.provider

// OIDC discovery を 1 プロセスにつき 1 度だけ取得してキャッシュする。
// token_endpoint は Keycloak (/protocol/openid-connect/token) と Cognito (hosted-UI ドメインの
// /oauth2/token) でパスが違うため、 ハードコードせず discovery から引いて provider 差を吸収する。
type OidcMetadata = {
  authorization_endpoint: string
  token_endpoint: string
  end_session_endpoint?: string
}

let metadataCache: Promise<OidcMetadata> | null = null

function oidcMetadata(): Promise<OidcMetadata> {
  if (!metadataCache) {
    metadataCache = fetch(`${active.issuer}/.well-known/openid-configuration`)
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(`OIDC discovery に失敗しました (status ${res.status}, issuer ${active.issuer})`)
        }
        return res.json() as Promise<OidcMetadata>
      })
      .catch((e) => {
        // 失敗した Promise をキャッシュし続けると以後ずっと同じ失敗を返すため、 リセットして次回リトライさせる。
        metadataCache = null
        throw e
      })
  }
  return metadataCache
}

export type RefreshResult =
  | { ok: true; data: { access_token: string; refresh_token?: string; expires_in: number } }
  | { ok: false; status: number; data: unknown }

/** refresh_token で access token を更新する。 token endpoint は discovery 由来なので provider 非依存。 */
export async function refreshAccessToken(refreshToken: string): Promise<RefreshResult> {
  const { token_endpoint } = await oidcMetadata()

  const headers: Record<string, string> = {
    "Content-Type": "application/x-www-form-urlencoded",
  }
  const body = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: active.clientId ?? "",
    refresh_token: refreshToken,
  })

  // secret 付き (confidential) client は Basic 認証で client 資格を送る ── Keycloak / Cognito の
  // token endpoint 共通の作法。 secret 無し (public) client は body の client_id だけで認証する。
  if (active.clientSecret) {
    headers.Authorization = "Basic " + btoa(`${active.clientId}:${active.clientSecret}`)
  }

  const res = await fetch(token_endpoint, { method: "POST", headers, body })
  const data = await res.json()
  if (!res.ok) {
    return { ok: false, status: res.status, data }
  }
  return { ok: true, data }
}

/**
 * sign-out 用の IdP ログアウト URL を組み立てる。 logout だけは仕様が provider 固有:
 * - Keycloak: OIDC RP-initiated logout (end_session_endpoint + id_token_hint + post_logout_redirect_uri)
 * - Cognito : end_session_endpoint は discovery に存在するが、 標準の RP-initiated logout とは引数が異なり
 *             client_id + logout_uri を要求する独自仕様。 よって hosted-UI ドメインの
 *             /logout?client_id=&logout_uri= を組み立てる (ドメインは authorization_endpoint の origin から導出)。
 */
export async function buildLogoutUrl(params: {
  idToken?: string
  postLogoutRedirectUri: string
}): Promise<string> {
  const meta = await oidcMetadata()

  if (PROVIDER === "keycloak") {
    const url = new URL(meta.end_session_endpoint!)
    if (params.idToken) url.searchParams.set("id_token_hint", params.idToken)
    url.searchParams.set("post_logout_redirect_uri", params.postLogoutRedirectUri)
    return url.toString()
  }

  // Cognito: /logout は authorization_endpoint と同じ hosted-UI ドメインの origin にある。
  // logout_uri は Cognito app client の「許可されているサインアウト URL」に登録が必要。
  const origin = new URL(meta.authorization_endpoint).origin
  const url = new URL(`${origin}/logout`)
  url.searchParams.set("client_id", active.clientId ?? "")
  url.searchParams.set("logout_uri", params.postLogoutRedirectUri)
  return url.toString()
}
