package jp.momiji.feature.product.list

import jp.momiji.feature.product.productStatusFilterFromProto
import jp.momiji.feature.product.productStatusToProto
import jp.momiji.grpc.momiji.common.v1.paging
import jp.momiji.grpc.momiji.product.list.v1.ListProductsRequest
import jp.momiji.grpc.momiji.product.list.v1.ListProductsResponse
import jp.momiji.grpc.momiji.product.list.v1.ListProductsServiceGrpcKt
import jp.momiji.grpc.momiji.product.list.v1.listProductsResponse
import jp.momiji.grpc.momiji.product.list.v1.product
import jp.momiji.grpc.momiji.product.v1.ProductSortCondition
import jp.momiji.query.PagingCondition
import jp.momiji.util.toProtoTimestamp
import org.springframework.stereotype.Service

@Service
class ListProductsGrpcService(
    private val listProductsQueryService: ListProductsQueryService,
) : ListProductsServiceGrpcKt.ListProductsServiceCoroutineImplBase() {
    override suspend fun listProducts(request: ListProductsRequest): ListProductsResponse {
        val query =
            ListProductsQuery(
                likeName = request.likeName,
                status = productStatusFilterFromProto(request.status),
                brandId = request.brandId,
                sort = request.sort.toProductSort(),
                paging = PagingCondition.of(request.pageSize, request.pageNumber),
            )
        val page = listProductsQueryService.list(query)

        return listProductsResponse {
            paging =
                paging {
                    totalCount = page.paging.totalCount
                    totalPage = page.paging.totalPage
                    pageSize = page.paging.pageSize
                    pageNumber = page.paging.pageNumber
                }
            products.addAll(
                page.items.map { view ->
                    product {
                        id = view.id
                        brandId = view.brandId
                        name = view.name
                        description = view.description
                        imageUrl = view.imageUrl ?: ""
                        price = view.price
                        status = productStatusToProto(view.status)
                        createdAt = view.createdAt.toProtoTimestamp()
                        updatedAt = view.updatedAt.toProtoTimestamp()
                    }
                },
            )
        }
    }

    private fun ProductSortCondition.toProductSort(): ProductSort =
        when (this) {
            ProductSortCondition.PRODUCT_SORT_CONDITION_NAME_ASC -> ProductSort.NAME_ASC
            ProductSortCondition.PRODUCT_SORT_CONDITION_NAME_DESC -> ProductSort.NAME_DESC
            ProductSortCondition.PRODUCT_SORT_CONDITION_PRICE_ASC -> ProductSort.PRICE_ASC
            ProductSortCondition.PRODUCT_SORT_CONDITION_PRICE_DESC -> ProductSort.PRICE_DESC
            ProductSortCondition.PRODUCT_SORT_CONDITION_CREATED_AT_DESC -> ProductSort.CREATED_AT_DESC
            ProductSortCondition.PRODUCT_SORT_CONDITION_CREATED_AT_ASC -> ProductSort.CREATED_AT_ASC
            // UNSPECIFIED / 未知の値は既定（名前昇順）に倒す
            else -> ProductSort.NAME_ASC
        }
}
