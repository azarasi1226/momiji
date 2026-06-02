package jp.momiji.feature.idp

import io.mockk.every
import io.mockk.mockk
import jp.momiji.domain.ValidationException
import jp.momiji.domain.idp.IdentityProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CognitoUserInfoFetcher の単体テスト。 AdminGetUser のレスポンス ( ユーザー属性 ) をモックして、
 * email_verified ( 文字列 ) の boolean 化 / identities からの IDP 解決 / email の値オブジェクト化を検証する。
 */
class CognitoUserInfoFetcherTest {
    private val cognitoClient = mockk<CognitoIdentityProviderClient>()
    private val fetcher = CognitoUserInfoFetcher("pool-1", cognitoClient)

    private fun stubAttributes(vararg attrs: Pair<String, String>) {
        every { cognitoClient.adminGetUser(any<AdminGetUserRequest>()) } returns
            AdminGetUserResponse
                .builder()
                .userAttributes(
                    attrs.map { (name, value) ->
                        AttributeType
                            .builder()
                            .name(name)
                            .value(value)
                            .build()
                    },
                ).build()
    }

    @Test
    fun `正常系_identities が無ければ LOCAL、 email_verified 文字列を boolean 化する`() {
        stubAttributes(
            "email" to "alice@example.com",
            "email_verified" to "true",
        )

        val result = fetcher.handle("subj-1", "https://idp.example.com")

        assertEquals("subj-1", result.subject)
        assertEquals("https://idp.example.com", result.issuer)
        assertEquals("alice@example.com", result.email.value)
        assertTrue(result.emailVerified)
        assertEquals(IdentityProvider.LOCAL, result.identityProvider)
    }

    @Test
    fun `正常系_identities の providerType から IDP を解決する`() {
        stubAttributes(
            "email" to "alice@example.com",
            "email_verified" to "true",
            "identities" to """[{"providerType":"Google","providerName":"Google","userId":"x","primary":"true"}]""",
        )

        assertEquals(IdentityProvider.GOOGLE, fetcher.handle("subj-1", "https://idp.example.com").identityProvider)
    }

    @Test
    fun `異常系_email 属性が無い( 属性マッピング漏れ等 )なら ValidationException`() {
        stubAttributes("email_verified" to "true")

        assertThrows<ValidationException> { fetcher.handle("subj-1", "https://idp.example.com") }
    }
}
