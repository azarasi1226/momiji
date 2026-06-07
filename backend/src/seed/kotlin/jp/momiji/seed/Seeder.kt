package jp.momiji.seed

import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import jp.momiji.grpc.momiji.brand.archive.v1.ArchiveBrandRequest
import jp.momiji.grpc.momiji.brand.archive.v1.ArchiveBrandServiceGrpc
import jp.momiji.grpc.momiji.brand.create.v1.CreateBrandRequest
import jp.momiji.grpc.momiji.brand.create.v1.CreateBrandServiceGrpc
import jp.momiji.grpc.momiji.product.create.v1.CreateProductRequest
import jp.momiji.grpc.momiji.product.create.v1.CreateProductServiceGrpc
import jp.momiji.grpc.momiji.product.discontinue.v1.DiscontinueProductRequest
import jp.momiji.grpc.momiji.product.discontinue.v1.DiscontinueProductServiceGrpc
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * ローカル backend にテストデータ（brand / product）を投入するツール。
 *
 * 設計（→ なぜこの形か）:
 * - **ES なので read DB に直接 INSERT しない**。 起動中の backend に gRPC で command を撃ち、
 *   events → projection の正規ルートで作る（read model と event store が常に整合する）。
 * - brand/product の gRPC は認証必須（azp == "momiji" を検証）。 そこで **`momiji` クライアントの
 *   `client_credentials`** でトークンを取る（azp が "momiji" になり検証を通る）。
 * - id は**固定の決定的 ULID**にしてあるので create は冪等。 **何度流しても重複しない**（再実行安全）。
 */
private const val KEYCLOAK_TOKEN_URL =
    "http://localhost:8085/realms/momiji/protocol/openid-connect/token"
private const val GRPC_HOST = "localhost"
private const val GRPC_PORT = 9091
private const val CLIENT_ID = "momiji"
private const val CLIENT_SECRET = "momiji-client-secret"

private const val BRAND_COUNT = 5
private const val PRODUCTS_PER_BRAND = 20

fun main() {
    val token = fetchAccessToken()
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
        val archiveBrandStub = ArchiveBrandServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val productStub = CreateProductServiceGrpc.newBlockingStub(channel).withInterceptors(auth)
        val discontinueStub =
            DiscontinueProductServiceGrpc.newBlockingStub(channel).withInterceptors(auth)

        for (i in 1..BRAND_COUNT) {
            val brandId = brandId(i)
            brandStub.createBrand(
                CreateBrandRequest
                    .newBuilder()
                    .setId(brandId)
                    .setName("ブランド$i")
                    .setDescription("ブランド$i の説明文です。")
                    .build(),
            )

            for (j in 1..PRODUCTS_PER_BRAND) {
                val productId = productId(i, j)
                val builder =
                    CreateProductRequest
                        .newBuilder()
                        .setId(productId)
                        .setBrandId(brandId)
                        .setName("商品 B$i-P$j")
                        .setDescription("ブランド$i の商品$j の説明文です。")
                        .setPrice(500 + j * 100)
                // 画像URLは一部だけ（任意項目の有無をデータに散らす）
                if (j % 3 == 0) {
                    builder.imageUrl = "https://example.com/products/$i-$j.png"
                }
                productStub.createProduct(builder.build())

                // 各ブランドの末尾2件を生産終了にして DISCONTINUED の例を作る
                if (j > PRODUCTS_PER_BRAND - 2) {
                    discontinueStub.discontinueProduct(
                        DiscontinueProductRequest.newBuilder().setId(productId).build(),
                    )
                }
            }
            println("seeded brand $i ($brandId) with $PRODUCTS_PER_BRAND products")
        }

        // 最後のブランドをアーカイブ（紐づく商品は残る = ライフサイクルの確認用 ARCHIVED 例）。
        // 商品作成後に実行する（作成時はブランドが ACTIVE である必要があるため）。
        archiveBrandStub.archiveBrand(
            ArchiveBrandRequest.newBuilder().setId(brandId(BRAND_COUNT)).build(),
        )
        println("archived brand $BRAND_COUNT")
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
