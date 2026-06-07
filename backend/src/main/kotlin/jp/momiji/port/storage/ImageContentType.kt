package jp.momiji.port.storage

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import jp.momiji.domain.ValidationError

/**
 * アップロードを許可する画像の MIME 種別。 拡張子はオブジェクトキー組み立てに使う。
 *
 * これは「業務（商品）の不変条件」ではなく **ストレージが受け付けるメディア種別という技術ポリシー**なので
 * domain ではなくストレージ契約（port/storage）側に置く。 presigned PUT では content-type を署名に含めて
 * 固定するため、 許可リスト外は発行段階で弾く。
 */
enum class ImageContentType(
    val mime: String,
    val extension: String,
) {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp"),
    ;

    companion object {
        fun fromMime(input: String): Result<ImageContentType, ValidationError> =
            entries.find { it.mime == input }?.let { Ok(it) } ?: Err(Unsupported)
    }

    object Unsupported : ValidationError("contentType", "対応していない画像形式です（png / jpeg / webp）")
}
