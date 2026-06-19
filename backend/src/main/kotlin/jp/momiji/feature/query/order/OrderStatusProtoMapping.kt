package jp.momiji.feature.query.order

import jp.momiji.domain.order.OrderStatus
import jp.momiji.grpc.momiji.order.v1.OrderStatus as ProtoOrderStatus

/**
 * read model の status 文字列（= [OrderStatus] の name）を gRPC の proto enum に変換する。
 *
 * ドメイン enum を経由する（`valueOf`）ことで、 DB に想定外の値が入っていれば早期に例外で気付ける
 * （stringly-typed のまま透過しない）。 [ProductStatusProtoMapping][jp.momiji.feature.command.product] と同じ方針。
 */
internal fun orderStatusToProto(dbValue: String): ProtoOrderStatus =
    when (OrderStatus.valueOf(dbValue)) {
        OrderStatus.STARTED -> ProtoOrderStatus.ORDER_STATUS_STARTED
        OrderStatus.PAYMENT_PENDING -> ProtoOrderStatus.ORDER_STATUS_PAYMENT_PENDING
        OrderStatus.PAID -> ProtoOrderStatus.ORDER_STATUS_PAID
        OrderStatus.SHIPPED -> ProtoOrderStatus.ORDER_STATUS_SHIPPED
        OrderStatus.COMPLETED -> ProtoOrderStatus.ORDER_STATUS_COMPLETED
        OrderStatus.FAILED -> ProtoOrderStatus.ORDER_STATUS_FAILED
        OrderStatus.CANCELLED -> ProtoOrderStatus.ORDER_STATUS_CANCELLED
    }
