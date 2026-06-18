package jp.momiji.feature.command.order.complete

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.event.order.OrderShippedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.InitialPosition
import jp.momiji.feature.command.order.OrderProductIdsReader
import jp.momiji.feature.command.pooledStreamingProcessorFor
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 発送（[OrderShippedEvent]）を拾い、 注文を完了させる reactor（ADR 0013）。
 *
 * PM は「イベント → コマンド」の薄い中継に徹する（状態・不変条件を持たない）。 不変条件（SHIPPED のときだけ完了）は
 * 撃った先の CommandHandler が ORDER_ID の DCB sourcing で守る（fire → guard）。 [CompleteOrderCommand] は冪等
 * （COMPLETED 以降 no-op）なので、 イベント再処理・リトライで二重に撃たれても安全。
 *
 * processor は reactor（＝現実のコマンド発行という副作用）なので **pooledStreaming + LATEST**・**固定名**で登録する
 * （履歴を再生して過去オーダーを今ごろ完了させる事故を避ける。 ADR 0013 / 0009）。
 */
@Component
class CompleteShippedOrderProcessManager(
    private val commandGateway: CommandGateway,
    private val orderProductIdsReader: OrderProductIdsReader,
) {
    @EventHandler
    fun on(event: OrderShippedEvent) {
        // 注文完了（SHIPPED → COMPLETED）＋ 在庫の引き当て確定。 ガードは CompleteOrderCommandHandler。
        // ProductsState の整合境界（product_id）を組むため、 order_items（read model）から productId を読む（FailOrder の driver と同じ）。
        val command = CompleteOrderCommand(orderId = event.orderId, productIds = orderProductIdsReader.read(event.orderId))

        // インフラ障害は .get() が throw → processor がリトライ（CompleteOrder は冪等で再実行安全）。
        // 業務エラー（success == false）は永続的な異常なのでログのみ（リトライループにしない）。
        val result = commandGateway.send(command, CommandResult::class.java).get()
        if (!result.success) {
            logger.warn { "発送完了 PM: 注文完了 order=${event.orderId} が業務エラー: ${result.error?.message}" }
        }
    }

    @Configuration
    class Config {
        @Bean
        fun completeShippedOrderProcessManagerProcessor() =
            pooledStreamingProcessorFor<CompleteShippedOrderProcessManager>("shipped-order-completion", InitialPosition.LATEST)
    }
}
