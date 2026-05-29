package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

data class PostalCode internal constructor(
    val value: String,
) {
    companion object {
        // 日本の郵便番号 ハイフン区切り (例: 100-0000)
        private val PATTERN = Regex("""^\d{3}-\d{4}$""")

        fun create(input: String): Result<PostalCode, DomainError> {
            if (!PATTERN.matches(input)) return Err(Invalid)
            return Ok(PostalCode(input))
        }

        object Invalid : DomainError("postalCode", "郵便番号は ハイフン区切り (例: 100-0000) で入力してください")
    }
}
