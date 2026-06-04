package jp.momiji.port.idp

interface IdpUserClient {
    fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    )

    fun deleteUser(oidcSubject: String)
}
