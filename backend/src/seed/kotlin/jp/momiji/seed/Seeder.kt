package jp.momiji.seed

import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import jp.momiji.grpc.momiji.brand.archive.ArchiveBrandRequest
import jp.momiji.grpc.momiji.brand.archive.ArchiveBrandServiceGrpc
import jp.momiji.grpc.momiji.brand.create.CreateBrandRequest
import jp.momiji.grpc.momiji.brand.create.CreateBrandServiceGrpc
import jp.momiji.grpc.momiji.image.issueuploadurl.IssueImageUploadUrlServiceGrpc
import jp.momiji.grpc.momiji.product.create.CreateProductRequest
import jp.momiji.grpc.momiji.product.create.CreateProductServiceGrpc
import jp.momiji.grpc.momiji.product.discontinue.DiscontinueProductRequest
import jp.momiji.grpc.momiji.product.discontinue.DiscontinueProductServiceGrpc
import jp.momiji.grpc.momiji.stock.receive.ReceiveStockRequest
import jp.momiji.grpc.momiji.stock.receive.ReceiveStockServiceGrpc
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * ローカル backend に**画像込みの実テストデータ（brand / product）**を投入するツール。
 *
 * 設計（→ なぜこの形か）:
 * - **ES なので read DB に直接 INSERT しない**。 起動中の backend に gRPC で command を撃ち、
 *   events → projection の正規ルートで作る（read model と event store が常に整合する）。
 * - brand/product/image の gRPC は認証必須（clientId == "momiji" を検証）。 そこで **`momiji` クライアントの
 *   `client_credentials`** でトークンを取る（azp が "momiji" になり検証を通る）。
 * - id は**固定の決定的 ULID**にしてあるので create は冪等（`CreateProductCommandHandler` は既存なら
 *   何もせず success を返す）。 **何度流しても商品は重複しない**（再実行安全）。
 *
 * 投入するデータ定義は [BRANDS]（[SeedData]）、 画像アップロードは [uploadImage]（[ImageUploader]）を参照。
 *
 * 注意: 再実行するとその都度 MinIO へ画像を上げ直す（product 自体は冪等だが、 過去の画像は
 * 参照されない孤児として残る）。 気になる場合は MinIO のボリュームごと作り直す。
 */
private const val KEYCLOAK_TOKEN_URL =
    "http://localhost:8085/realms/momiji/protocol/openid-connect/token"
private const val GRPC_HOST = "localhost"
private const val GRPC_PORT = 9091
private const val CLIENT_ID = "momiji"
private const val CLIENT_SECRET = "momiji-client-secret"

fun main() {
    val token = fetchAccessToken()
    val httpClient = HttpClient.newHttpClient()
    val channel =
        ManagedChannelBuilder
            .forAddress(GRPC_HOST, GRPC_PORT)
            .usePlaintext()
            .build()

    try {
        val auth =
            MetadataUtils.newAttachHeadersInterceptor(
                Metadata().apply {
                    put(
                        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "Bearer $token",
                    )
                },
            )
        val brandStub = CreateBrandServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val archiveBrandStub =
            ArchiveBrandServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val productStub = CreateProductServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val discontinueStub =
            DiscontinueProductServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val receiveStockStub =
            ReceiveStockServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val imageStub =
            IssueImageUploadUrlServiceGrpc.newBlockingStub(channel).withInterceptors(auth)

        BRANDS.forEachIndexed { brandIndex, brand ->
            val brandId = brandId(brandIndex + 1)
            brandStub.createBrand(
                CreateBrandRequest
                    .newBuilder()
                    .setId(brandId)
                    .setName(brand.name)
                    .setDescription(brand.description)
                    .build(),
            )

            brand.products.forEachIndexed { productIndex, product ->
                val productId = productId(brandIndex + 1, productIndex + 1)

                // 画像を MinIO に上げ、 表示用の public URL を得る（本番と同じ presigned PUT 経路）。
                val imageUrl = uploadImage(imageStub, httpClient, brand.folder, product.imageFile)

                productStub.createProduct(
                    CreateProductRequest
                        .newBuilder()
                        .setId(productId)
                        .setBrandId(brandId)
                        .setName(product.name)
                        .setDescription(product.description)
                        .setImageUrl(imageUrl)
                        .setPrice(product.price)
                        .build(),
                )

                // stock = 0 は在庫切れの例。 入庫しない（在庫行を作らない）＝クエリ側で 0 扱いになる。
                if (product.stock > 0) {
                    receiveStockStub.receiveStock(
                        ReceiveStockRequest
                            .newBuilder()
                            .setProductId(productId)
                            .setQuantity(product.stock)
                            .build(),
                    )
                }

                // 生産終了（DISCONTINUED）の例。 作成後に切り替える。
                if (product.discontinued) {
                    discontinueStub.discontinueProduct(
                        DiscontinueProductRequest.newBuilder().setId(productId).build(),
                    )
                }
                println("  seeded product ${product.name}")
            }

            // アーカイブ（ARCHIVED）の例。 商品作成後に実行する（作成時はブランドが ACTIVE である必要があるため）。
            if (brand.archived) {
                archiveBrandStub.archiveBrand(
                    ArchiveBrandRequest.newBuilder().setId(brandId).build(),
                )
            }
            println("seeded brand ${brand.name} ($brandId) with ${brand.products.size} products")
        }
        println("done.")
    } finally {
        channel.shutdownNow()
    }
}

/** 決定的な ULID（26 文字・Crockford base32 の有効文字のみ）。 固定なので create が冪等になる。 */
private fun brandId(i: Int): String = "01JSEEDBRAND" + i.toString().padStart(14, '0')

private fun productId(
    i: Int,
    j: Int,
): String = "01JSEEDPRD" + (i * 100 + j).toString().padStart(16, '0')

private fun fetchAccessToken(): String {
    val form =
        "grant_type=client_credentials&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET"
    val request =
        HttpRequest
            .newBuilder(URI.create(KEYCLOAK_TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) {
        "Keycloak からのトークン取得に失敗しました: ${response.statusCode()} ${response.body()}"
    }
    return ObjectMapper().readTree(response.body()).get("access_token").asText()
}
