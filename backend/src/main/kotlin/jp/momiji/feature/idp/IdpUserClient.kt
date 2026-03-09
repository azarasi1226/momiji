package jp.momiji.feature.idp

interface IdpUserClient {
  fun updateEmail(oidcSubject: String, newEmail: String)
}
