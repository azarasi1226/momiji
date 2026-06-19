package jp.momiji.feature.query.product.list

import jp.momiji.feature.command.product.productStatusFilterFromProto
import jp.momiji.feature.command.product.productStatusToProto
import jp.momiji.feature.query.PagingCondition
import jp.momiji.grpc.momiji.common.paging
import jp.momiji.grpc.momiji.product.ProductSortCondition
import jp.momiji.grpc.momiji.product.list.ListProductsRequest
import jp.momiji.grpc.momiji.product.list.ListProductsResponse
import jp.momiji.grpc.momiji.product.list.ListProductsServiceGrpcKt
import jp.momiji.grpc.momiji.product.list.listProductsResponse
import jp.momiji.grpc.momiji.product.list.product
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
                inStockOnly = request.inStockOnly,
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
                page.items.map { row ->
                    val view = row.product
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
                        stockOnHand = row.onHand
                        stockReserved = row.reserved
                        stockAvailable = row.available
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
