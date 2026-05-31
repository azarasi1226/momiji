package jp.momiji.domain.idp

import jp.momiji.domain.UseCaseException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdentityProviderResolverTest {
    // 小文字 IDP 名を持つ resolver ( Keycloak 想定 )
    private val lowerCaseResolver =
        object : IdentityProviderResolver() {
            override val googleProviderName = "google"
        }

    // 大文字始まり IDP 名を持つ resolver ( Cognito 想定 )
    private val capitalizedResolver =
        object : IdentityProviderResolver() {
            override val googleProviderName = "Google"
        }

    @Test
    fun `登録した名前と完全一致なら該当 IDP を返す`() {
        assertEquals(IdentityProvider.GOOGLE, lowerCaseResolver.resolve("google"))
        assertEquals(IdentityProvider.GOOGLE, capitalizedResolver.resolve("Google"))
    }

    @Test
    fun `登録した名前と case が違うとマッチしないので fail-closed で例外`() {
        assertFailsWith<UseCaseException> { lowerCaseResolver.resolve("Google") }
        assertFailsWith<UseCaseException> { capitalizedResolver.resolve("google") }
    }

    @Test
    fun `whitelist に無い名前 ( facebook ) は fail-closed で例外`() {
        assertFailsWith<UseCaseException> { lowerCaseResolver.resolve("facebook") }
        assertFailsWith<UseCaseException> { capitalizedResolver.resolve("Facebook") }
    }

    @Test
    fun `空文字も whitelist に無いので例外`() {
        assertFailsWith<UseCaseException> { lowerCaseResolver.resolve("") }
    }

    @Test
    fun `LOCAL は外部名を持たないので、 どの文字列を渡しても LOCAL にはマッチせず例外`() {
        // LOCAL の externalNameOf は null。 firstOrNull の predicate で null == "anything" は常に false。
        // つまり「外部名から LOCAL を引き当てる」 ことはできず、 LOCAL は呼び出し側の null ガード節で扱う設計。
        for (input in listOf("local", "LOCAL", "null", "none")) {
            assertFailsWith<UseCaseException> { lowerCaseResolver.resolve(input) }
        }
    }
}
