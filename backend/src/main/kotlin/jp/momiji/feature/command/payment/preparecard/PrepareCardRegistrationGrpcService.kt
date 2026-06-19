package jp.momiji.feature.command.payment.preparecard

import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.payment.preparecard.PrepareCardRegistrationRequest
import jp.momiji.grpc.momiji.payment.preparecard.PrepareCardRegistrationResponse
import jp.momiji.grpc.momiji.payment.preparecard.PrepareCardRegistrationServiceGrpcKt
import jp.momiji.grpc.momiji.payment.preparecard.prepareCardRegistrationResponse
import jp.momiji.port.payment.PaymentGateway
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

/**
 * カード登録準備の入口。 前半（同期）を担う。
 *
 * 1. JWT から userId を解決
 * 2. 既存 cus_ があれば再利用、 無ければ Stripe Customer を lazy 作成し記録コマンドを送る
 * 3. その Customer で SetupIntent を作り、 client_secret をフロントへ返す
 *
 * 後半（webhook `setup_intent.succeeded`）でカードが確定し、 [jp.momiji.event.payment.CardRegisteredEvent] になる。
 */
@Service
class PrepareCardRegistrationGrpcService(
    private val commandGateway: CommandGateway,
    private val userIdResolver: UserIdResolver,
    private val stripeCustomerReader: StripeCustomerReader,
    private val paymentGateway: PaymentGateway,
) : PrepareCardRegistrationServiceGrpcKt.PrepareCardRegistrationServiceCoroutineImplBase() {
    override suspend fun prepareCardRegistration(request: PrepareCardRegistrationRequest): PrepareCardRegistrationResponse {
        val accessToken = GrpcAuthContext.current().token
        val userId = userIdResolver.resolve(accessToken)

        // lazy Customer: 既存があれば再利用、 無ければ新規作成。
        //
        // Stripe への Customer 作成とイベント記録はトランザクションで括れないため、 ReadModel が Projection される前 でも
        // Stripe 側には既に Customer が存在する瞬間がある。 危険な香りは次の2つ:
        //   (a) 並走・連打: projection（非同期）の反映前に 2 回目の 本処理 が findByUserId() で null を読む　→　再度 createAndRecordCustomerを呼ぶ。
        //   (b) 作成成功 → 記録コマンド失敗（クラッシュ等）: イベントが残らず ReadModel は永遠に null　→　再度　createAndRecordCustomerを呼ぶ。
        //
        // そのまま作り直すと Customer が二重作成されるため、 二段構えで防ぐ:
        //   ① stripeCustomerReader: 記録に成功した経路を恒久的に守る（projection 反映後は二度と作成しない）
        //   ② createCustomer の Idempotency-Key（userId 固定）: ①が効かない (a)(b) でも、 24 時間以内なら
        //      Stripe が同一 Customer を返すので二重作成にならない
        // (b) が 24 時間を超えた場合のみ②もすり抜けて孤児 Customer が Stripe に残るが、
        // 空の Customer（カード未 attach・課金不可）なので実害なしと割り切るしかない....悲しいことですが...
        val customerId = stripeCustomerReader.findByUserId(userId) ?: createAndRecordCustomer(userId)

        val secret = paymentGateway.createSetupIntent(stripeCustomerId = customerId, userId = userId)

        return prepareCardRegistrationResponse {
            clientSecret = secret
        }
    }

    /**
     * Stripe Customer を新規作成し、 その事実（user ↔ cus_ のリンク）をイベントとして記録する。
     */
    private suspend fun createAndRecordCustomer(userId: String): String {
        val customerId = paymentGateway.createCustomer(userId)
        commandGateway
            .prepareCardRegistration(
                PrepareCardRegistrationCommand(userId = userId, stripeCustomerId = customerId),
            ).throwIfError()
        return customerId
    }
}
