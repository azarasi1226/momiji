package jp.momiji.feature.command.order.preparepayment

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.order.preparepayment.PreparePaymentRequest
import jp.momiji.grpc.momiji.order.preparepayment.PreparePaymentResponse
import jp.momiji.grpc.momiji.order.preparepayment.PreparePaymentServiceGrpcKt
import jp.momiji.grpc.momiji.order.preparepayment.preparePaymentResponse
import jp.momiji.port.payment.PaymentGateway
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

/**
 * 注文の決済準備の入口（前半=同期）。 PrepareCardRegistration と同じ二相パターンを PaymentIntent でなぞる。
 *
 * 1. 注文・カードの所有権・状態を検証（[PayableOrderReader]）
 * 2. 合計金額（注文時点スナップショット）で PaymentIntent を作成（外部・冪等）
 * 3. [PreparePaymentCommand] で PAYMENT_PENDING へ記録（pi_/pm_）
 * 4. client_secret をフロントへ返す → Stripe.js で confirm/3DS → webhook で確定
 */
@Service
class PreparePaymentGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
    private val paymentGateway: PaymentGateway,
    private val payableOrderReader: PayableOrderReader,
) : PreparePaymentServiceGrpcKt.PreparePaymentServiceCoroutineImplBase() {
    override suspend fun preparePayment(request: PreparePaymentRequest): PreparePaymentResponse {
        val userId = userIdResolver.resolve(GrpcAuthContext.current().token)

        // 注文・カード・Customer・合計金額の存在確認と所有権検証
        val payable =
            payableOrderReader.loadForPayment(
                orderId = request.orderId,
                userId = userId,
                paymentMethodId = request.paymentMethodId,
            )

        // PaymentIntent 作成
        val paymentIntent =
            paymentGateway.createPaymentIntent(
                stripeCustomerId = payable.stripeCustomerId,
                paymentMethodId = request.paymentMethodId,
                amount = payable.totalAmount,
                orderId = request.orderId,
            )

        // PAYMENT_PENDING へ遷移を記録
        // PaymentIntent は冪等に作られているので、 commandGateway と同一のトランザクションに括っていないが問題は無い認識。
        // もし、複数回この grpcService.handle が叩かれたとしても、２重請求になることは無い。
        // また、paymentIntent も24時間でセッションが切れるため、複数回実行されてもゴミデータはstripe側にたまらない。
        commandGateway
            .preparePayment(
                PreparePaymentCommand(
                    orderId = request.orderId,
                    paymentMethodId = request.paymentMethodId,
                    paymentIntentId = paymentIntent.paymentIntentId,
                    // PI を作った課金額。 handler が注文時点の権威合計と照合する（不一致なら client_secret を返さない）。
                    amount = payable.totalAmount,
                ),
            ).throwIfError()

        return preparePaymentResponse { clientSecret = paymentIntent.clientSecret }
    }
}
