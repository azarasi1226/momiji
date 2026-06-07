package jp.momiji.feature.command.product

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

/**
 * gRPC の proto enum を**状態フィルタ**のドメイン enum に変換する（一覧の絞り込み用）。
 * UNSPECIFIED / 未知の値は `null`（= 絞り込みなし＝すべて）にする。
 */
internal fun productStatusFilterFromProto(proto: ProtoProductStatus): ProductStatus? =
    when (proto) {
        ProtoProductStatus.PRODUCT_STATUS_ACTIVE -> ProductStatus.ACTIVE
        ProtoProductStatus.PRODUCT_STATUS_DISCONTINUED -> ProductStatus.DISCONTINUED
        else -> null
    }
