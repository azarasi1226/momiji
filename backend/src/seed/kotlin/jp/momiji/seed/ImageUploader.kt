package jp.momiji.seed

import jp.momiji.grpc.momiji.image.issueuploadurl.IssueImageUploadUrlRequest
import jp.momiji.grpc.momiji.image.issueuploadurl.IssueImageUploadUrlServiceGrpc
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/*
 * 画像素材を MinIO へアップロードするヘルパー。
 *
 * バイナリは本番と同じ presigned PUT 経路で投入する（`IssueImageUploadUrl` で URL を発行 → その URL へ
 * ブラウザ相当の直接 PUT）。 得た public URL を `createProduct` の image_url に渡す。
 */

/** 画像素材の置き場。 `seedData` タスクの作業ディレクトリ（= backend プロジェクトルート）からの相対。 */
private val IMAGES_DIR = File("src/seed/resources/images")

/**
 * 画像 1 枚を MinIO へアップロードし、 保存・表示に使う public URL を返す。
 * 1) `IssueImageUploadUrl` で presigned PUT URL + public URL を取得
 * 2) 画像バイナリを presigned URL へ HTTP PUT（署名に含まれる content-type を一致させる）
 */
internal fun uploadImage(
    imageStub: IssueImageUploadUrlServiceGrpc.IssueImageUploadUrlServiceBlockingStub,
    httpClient: HttpClient,
    brandFolder: String,
    imageFile: String,
): String {
    val file = File(IMAGES_DIR, "$brandFolder/$imageFile")
    check(file.exists()) { "画像が見つかりません: ${file.absolutePath}" }
    val contentType = mimeOf(imageFile)

    val issued =
        imageStub.issueImageUploadUrl(
            IssueImageUploadUrlRequest.newBuilder().setContentType(contentType).build(),
        )

    val putRequest =
        HttpRequest
            .newBuilder(URI.create(issued.uploadUrl))
            .header("Content-Type", contentType)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(file.readBytes()))
            .build()
    val putResponse = httpClient.send(putRequest, HttpResponse.BodyHandlers.ofString())
    check(putResponse.statusCode() in 200..299) {
        "画像アップロードに失敗しました ($imageFile): ${putResponse.statusCode()} ${putResponse.body()}"
    }

    return issued.publicUrl
}

/** 拡張子から MIME を決める（許可種別は backend の ImageContentType と一致させる）。 */
private fun mimeOf(fileName: String): String =
    when (fileName.substringAfterLast('.').lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> error("対応していない画像形式です: $fileName")
    }
