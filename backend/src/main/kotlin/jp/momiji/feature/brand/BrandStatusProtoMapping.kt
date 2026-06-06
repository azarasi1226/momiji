package jp.momiji.feature.brand

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.grpc.momiji.brand.v1.BrandStatus as ProtoBrandStatus

internal fun brandStatusToProto(dbValue: String): ProtoBrandStatus =
    when (BrandStatus.valueOf(dbValue)) {
        BrandStatus.ACTIVE -> ProtoBrandStatus.BRAND_STATUS_ACTIVE
        BrandStatus.ARCHIVED -> ProtoBrandStatus.BRAND_STATUS_ARCHIVED
    }
