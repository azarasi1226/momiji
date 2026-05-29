package jp.momiji.domain.user

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.DomainError

data class PhoneNumber internal constructor(
    val value: String,
) {
    companion object {
        // 国内番号 (市外局番 2-4 桁 - 市内局番 2-4 桁 - 加入者番号 4 桁) を ハイフン区切りで受ける
        private val PATTERN = Regex("""^\d{2,4}-\d{2,4}-\d{4}$""")

        fun create(input: String): Result<PhoneNumber, DomainError> {
            if (!PATTERN.matches(input)) return Err(Invalid)
            return Ok(PhoneNumber(input))
        }
    }

    object Invalid : DomainError("phoneNumber", "電話番号は ハイフン区切り (例: 090-0000-0000) で入力してください")
}
