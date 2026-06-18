package jp.momiji.feature.command.order.start

import jp.momiji.event.order.OrderStartedEvent
import jp.momiji.event.stock.StockReservedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.order.OrderState
import jp.momiji.feature.command.order.ProductsState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

/**
 * 注文開始の CommandHandler。 整合境界を 3 つの DCB エンティティに分けて注入する:
 * - [OrderState]（order_id）: 冪等判定
 * - [ShippingAddressesState]（user_id）: 配送先の所有権検証＋スナップショット
 * - [ProductsState]（product_id ×n）: 各商品の ACTIVE 判定・在庫（oversell 防止）
 *
 * 3 エンティティの整合境界の和集合に対する append 条件で、 全商品の予約・所有権検証を atomic に確定する。
 */
@Component
class StartOrderCommandHandler {
    @CommandHandler
    fun handle(
        command: StartOrderCommand,
        @InjectEntity(idProperty = "id") order: OrderState,
        @InjectEntity(idProperty = "userId") addresses: ShippingAddressesState,
        @InjectEntity(idProperty = "productIds") products: ProductsState,
        eventAppender: EventAppender,
    ): CommandResult {
        if (command.items.isEmpty()) {
            return StartOrderCommandResult.emptyOrder()
        }
        // 冪等性: 同じ order_id が再処理されたら（Axon のコマンド再送等）新規イベントを出さず成功。
        // TODO: 冪等キーを利用した仕組みが必要
        if (!order.notStarted) {
            return StartOrderCommandResult.success()
        }

        // 配送先を先に確定しておく。オーダー中に配送先が変更されたり、消されたり舌としてもユーザーが確定したときの住所が守られるためだ。
        val shippingAddress =
            addresses.find(command.shippingAddressId)
                ?: return StartOrderCommandResult.shippingAddressNotFound()

        // 全商品が "販売停止" 状態でないことを検証する
        val notPurchasable = command.items.map { it.productId }.filter { !products.isActive(it) }
        if (notPurchasable.isNotEmpty()) {
            return StartOrderCommandResult.productNotFound(notPurchasable)
        }

        // 金額確認: client が見ていた合計と、 server の権威価格（注文時点スナップショット）で計算した合計が一致するか。
        // projection 遅延・価格変更で食い違ったら、 黙って別額で確定せず弾く（client は再確認する）
        val serverTotalAmount = command.items.sumOf { products.priceOf(it.productId).toLong() * it.quantity }
        if (serverTotalAmount != command.expectedTotalAmount) {
            return StartOrderCommandResult.amountMismatch()
        }

        // 在庫が足りること（available = on_hand - reserved）。 全商品まとめて判定（all-or-nothing）。
        val shortage = command.items.filter { products.available(it.productId) < it.quantity }.map { it.productId }
        if (shortage.isNotEmpty()) {
            return StartOrderCommandResult.outOfStock(shortage)
        }

        // OrderStarted ＋ StockReserved×n を同一コマンドで atomic に追記する。
        val stockReservedEvents =
            command.items.map { item ->
                StockReservedEvent(
                    productId = item.productId,
                    orderId = command.id,
                    quantity = item.quantity,
                    reservedQuantity = products.reservedOf(item.productId) + item.quantity,
                )
            }
        val orderStartedEvent =
            OrderStartedEvent(
                orderId = command.id,
                userId = command.userId,
                shippingAddress = shippingAddress,
                items =
                    command.items.map { item ->
                        OrderStartedEvent.SnapshotItem(
                            productId = item.productId,
                            name = products.nameOf(item.productId),
                            unitPrice = products.priceOf(item.productId),
                            quantity = item.quantity,
                            imageUrl = products.imageUrlOf(item.productId),
                        )
                    },
            )
        eventAppender.append(orderStartedEvent, *stockReservedEvents.toTypedArray())

        return StartOrderCommandResult.success()
    }
}
