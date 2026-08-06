package jp.momiji.feature.query.order

import jp.momiji.grpc.momiji.order.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OrderStatusProtoMappingTest {
    @Test
    fun `全 status を proto enum へ変換する`() {
        assertEquals(OrderStatus.ORDER_STATUS_STARTED, orderStatusToProto("STARTED"))
        assertEquals(OrderStatus.ORDER_STATUS_PAYMENT_PENDING, orderStatusToProto("PAYMENT_PENDING"))
        assertEquals(OrderStatus.ORDER_STATUS_PAID, orderStatusToProto("PAID"))
        assertEquals(OrderStatus.ORDER_STATUS_SHIPPED, orderStatusToProto("SHIPPED"))
        assertEquals(OrderStatus.ORDER_STATUS_COMPLETED, orderStatusToProto("COMPLETED"))
        assertEquals(OrderStatus.ORDER_STATUS_FAILED, orderStatusToProto("FAILED"))
        assertEquals(OrderStatus.ORDER_STATUS_CANCELLED, orderStatusToProto("CANCELLED"))
    }

    @Test
    fun `未知の status 文字列は例外`() {
        assertThrows(IllegalArgumentException::class.java) { orderStatusToProto("UNKNOWN") }
    }
}
