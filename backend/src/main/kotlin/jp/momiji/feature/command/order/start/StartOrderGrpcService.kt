package jp.momiji.feature.command.order.start

import de.huxhorn.sulky.ulid.ULID
import iss.jooq.generated.tables.references.BASKETS
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.BusinessError
import jp.momiji.domain.BusinessException
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.order.start.v1.StartOrderRequest
import jp.momiji.grpc.momiji.order.start.v1.StartOrderResponse
import jp.momiji.grpc.momiji.order.start.v1.StartOrderServiceGrpcKt
import jp.momiji.grpc.momiji.order.start.v1.startOrderResponse
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class StartOrderGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
    private val dsl: DSLContext,
) : StartOrderServiceGrpcKt.StartOrderServiceCoroutineImplBase() {
    private val ulid = ULID()

    override suspend fun startOrder(request: StartOrderRequest): StartOrderResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        // カートの中身から注文明細を作る。 金額・個数はクライアントを信用しない (偽造される可能性があるため。)
        // CommandHandler の中で user_id をもとに、カートの中身を構築すればええやん。と思うやん？
        // その場合、 productの検証ができなくなるんだ。@EventCriteriaBuilder に指定できる product Idがないから。
        // だからここで カートの中身を取っている。　万が一ユーザーが見てる画面の内容と、ここで取れるカートの内容が食い違ったとしても
        // expectedTotalAmount というフィールドで合計金額が合っているかどうかを CommandHandler 内で検証してるから問題は無い。
        val items = readBasketItems(userId)
        if (items.isEmpty()) {
            throw BusinessException(BusinessError("カートが空です"))
        }

        // TODO: 冪等キーを利用した仕組みを導入する必要あり
        val orderId = ulid.nextULID()
        commandGateway
            .startOrder(
                StartOrderCommand(
                    id = orderId,
                    userId = userId,
                    shippingAddressId = request.shippingAddressId,
                    expectedTotalAmount = request.expectedTotalAmount,
                    items = items,
                ),
            ).throwIfError()

        return startOrderResponse { this.orderId = orderId }
    }

    private fun readBasketItems(userId: String): List<StartOrderCommand.Item> =
        dsl
            .select(BASKETS.PRODUCT_ID, BASKETS.ITEM_QUANTITY)
            .from(BASKETS)
            .where(BASKETS.USER_ID.eq(userId))
            .orderBy(BASKETS.ADDED_AT.asc())
            .fetch()
            .map { record ->
                StartOrderCommand.Item(
                    productId = record.value1()!!,
                    quantity = record.value2()!!,
                )
            }
}
