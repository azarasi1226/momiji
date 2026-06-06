package jp.momiji.feature.product

import jp.momiji.domain.product.ProductStatus
import jp.momiji.grpc.momiji.product.v1.ProductStatus as ProtoProductStatus

/**
 * read model の status 文字列（= [ProductStatus] の name）を gRPC の proto enum に変換する。
 *
 * findbyid / list の 2 サービスで共用。 ドメイン enum を経由する（`valueOf`）ことで、
 * DB に想定外の値が入っていれば早期に例外で気付ける（stringly-typed のまま透過しない）。
 */
internal fun productStatusToProto(dbValue: String): ProtoProductStatus =
    when (ProductStatus.valueOf(dbValue)) {
        ProductStatus.ACTIVE -> ProtoProductStatus.PRODUCT_STATUS_ACTIVE
        ProductStatus.DISCONTINUED -> ProtoProductStatus.PRODUCT_STATUS_DISCONTINUED
    }
