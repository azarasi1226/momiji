package jp.momiji.feature.idp

enum class IdentityProvider {
  /** Keycloak, Auth0, Cognitoなどの親元のIDPを表す */
  LOCAL,
  /** 親元のIDPと連携しているGoogleIDP */
  GOOGLE,
}

interface IdpUserClient {
  fun updateEmail(oidcSubject: String, newEmail: String)
  fun deleteUser(oidcSubject: String)
  fun getIdentityProvider(accessToken: String): IdentityProvider
}
