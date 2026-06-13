package jp.momiji.domain

import jp.momiji.domain.user.Name
import jp.momiji.domain.user.PhoneNumber
import jp.momiji.domain.user.StreetAddress
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationExceptionTest {
    @Test
    fun `単一エラーは field + message が message に乗る`() {
        val ex = ValidationException(listOf(Name.Blank))
        assertEquals("[name] 名前は必須です", ex.message)
    }

    @Test
    fun `複数エラーは ' _ ' (空白区切りスラッシュ) で join される`() {
        val ex =
            ValidationException(
                listOf(Name.Blank, PhoneNumber.Invalid, StreetAddress.Blank),
            )
        assertEquals(
            "[name] 名前は必須です / " +
                "[phoneNumber] 電話番号は ハイフン区切り (例: 090-0000-0000) で入力してください / " +
                "[streetAddress] 番地は必須です",
            ex.message,
        )
    }

    @Test
    fun `errors プロパティで個別アクセス可能`() {
        val errors = listOf(Name.Blank, PhoneNumber.Invalid)
        val ex = ValidationException(errors)
        assertEquals(errors, ex.errors)
    }

    @Test
    fun `Exception として throw 可能`() {
        val ex = ValidationException(listOf(Name.Blank))
        assertTrue(ex is Exception)
    }
}
