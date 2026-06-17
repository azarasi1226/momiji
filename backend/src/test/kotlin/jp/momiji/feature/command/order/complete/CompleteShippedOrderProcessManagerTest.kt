package jp.momiji.feature.command.order.complete

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.BusinessError
import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.feature.command.CommandResult
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

/**
 * 発送完了 PM が「OrderShipped → CompleteOrder」を撃つことだけを検証する純粋ユニットテスト。
 * 不変条件（SHIPPED のときだけ完了 等）・在庫確定は撃った先の CommandHandler 側でテストする（ADR 0013: fire → guard）。
 *
 * productIds の read model 取得（jOOQ）は relaxed mock で空リストを返させ、 ここではコマンド発行だけを検証する。
 */
class CompleteShippedOrderProcessManagerTest {
    private val commandGateway = mockk<CommandGateway>()
    private val dsl = mockk<DSLContext>(relaxed = true)
    private val pm = CompleteShippedOrderProcessManager(commandGateway, dsl)

    @Test
    fun `OrderShipped を受けて注文完了コマンドを撃つ`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        pm.on(OrderShippedEvent(orderId = "order-1"))

        verify(exactly = 1) {
            commandGateway.send(CompleteOrderCommand(orderId = "order-1", productIds = emptyList()), CommandResult::class.java)
        }
    }

    @Test
    fun `コマンドが業務エラーでも例外を投げない（リトライループにしない）`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.fail(BusinessError("完了できません")))

        // 例外を投げずに完了する（業務エラーはログのみ）。 投げると assertThrows せずともテストが失敗する。
        pm.on(OrderShippedEvent(orderId = "order-2"))

        verify(exactly = 1) {
            commandGateway.send(CompleteOrderCommand(orderId = "order-2", productIds = emptyList()), CommandResult::class.java)
        }
    }
}
