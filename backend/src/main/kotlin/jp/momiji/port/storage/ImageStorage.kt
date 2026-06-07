package jp.momiji.port.storage

/**
 * 画像ストレージへの外向きポート。 実装は local=MinIO / prod=S3（[jp.momiji.adapter.storage] で差し替え）。
 *
 * バイナリ自体はこのアプリを通さず、 ブラウザが [UploadTarget.uploadUrl]（presigned PUT）へ直接アップロードする。
 */
interface ImageStorage {
    /** アップロード用の presigned PUT URL と、 保存・表示に使う恒久 public URL を発行する。 */
    fun issueUploadUrl(contentType: ImageContentType): UploadTarget
}

data class UploadTarget(
    // ブラウザが PUT する先（presigned, 期限付き）。
    val uploadUrl: String,
    // DB に保存し <img> で表示する恒久 URL。
    val publicUrl: String,
)
