package jp.momiji.feature.command.order.cancel

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.domain.order.OrderCancellationReason
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.order.OrderOwnershipReader
import jp.momiji.feature.command.order.OrderProductIdsReader
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.order.cancel.v1.CancelOrderRequest
import jp.momiji.grpc.momiji.order.cancel.v1.CancelOrderResponse
import jp.momiji.grpc.momiji.order.cancel.v1.CancelOrderServiceGrpcKt
import jp.momiji.grpc.momiji.order.cancel.v1.CancellationReason
import jp.momiji.grpc.momiji.order.cancel.v1.cancelOrderResponse
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

/**
 * 注文キャンセルの入口（ユーザー起点）。
 *
 * 1. JWT から userId を解決し、 **本人の注文か**を read model（[OrderOwnershipReader]）で確認（他人の注文は弾く）。
 * 2. 理由（必須・UNSPECIFIED は拒否）をドメインに変換。
 * 3. 整合境界の product_id を read model から読み（[OrderProductIdsReader]）、 [CancelOrderCommand] を撃つ。
 *
 * 「発送済みでキャンセル不可」等の正しさは CommandHandler が authoritative にガードする（read model はルーティング）。
 */
@Service
class CancelOrderGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
    private val orderOwnershipReader: OrderOwnershipReader,
    private val orderProductIdsReader: OrderProductIdsReader,
) : CancelOrderServiceGrpcKt.CancelOrderServiceCoroutineImplBase() {
    override suspend fun cancelOrder(request: CancelOrderRequest): CancelOrderResponse {
        val userId = userIdResolver.resolve(GrpcAuthContext.current().token)

        // 本人の注文でなければ「見つかりません」（存在も漏らさない）。
        if (!orderOwnershipReader.isOwnedBy(request.orderId, userId)) {
            throw BusinessException(BusinessError("注文が見つかりません"))
        }

        val reason = request.reason.toDomain()
        val productIds = orderProductIdsReader.read(request.orderId)

        commandGateway
            .cancelOrder(
                CancelOrderCommand(
                    orderId = request.orderId,
                    productIds = productIds,
                    reason = reason,
                ),
            ).throwIfError()

        return cancelOrderResponse {}
    }

    // 理由は必須。 UNSPECIFIED / 未知は弾く（黙って既定理由に倒さない）。
    private fun CancellationReason.toDomain(): OrderCancellationReason =
        when (this) {
            CancellationReason.CANCELLATION_REASON_CHANGED_MIND -> OrderCancellationReason.CHANGED_MIND
            CancellationReason.CANCELLATION_REASON_ORDERED_BY_MISTAKE -> OrderCancellationReason.ORDERED_BY_MISTAKE
            CancellationReason.CANCELLATION_REASON_FOUND_BETTER_PRICE -> OrderCancellationReason.FOUND_BETTER_PRICE
            CancellationReason.CANCELLATION_REASON_DELIVERY_TOO_SLOW -> OrderCancellationReason.DELIVERY_TOO_SLOW
            CancellationReason.CANCELLATION_REASON_OTHER -> OrderCancellationReason.OTHER
            CancellationReason.CANCELLATION_REASON_UNSPECIFIED,
            CancellationReason.UNRECOGNIZED,
            -> throw BusinessException(BusinessError("キャンセル理由を選択してください"))
        }
}
