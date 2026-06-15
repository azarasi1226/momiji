package jp.momiji.port.payment

/**
 * 決済プロバイダ（Stripe）への外向きポート。
 *
 * カード登録は「前半=同期（Customer/SetupIntent 作成 → client_secret 返却）/ 後半=webhook（確定）」の二相。
 * このポートは外部 IO を抽象化し、 CommandHandler から切り離す（CommandHandler は事実の記録だけにする）。
 * 実装は [jp.momiji.adapter.payment.StripePaymentGateway]（`payment-stripe` profile）。
 */
interface PaymentGateway {
    /**
     * Stripe Customer（cus_）を新規作成し、 その ID を返す。
     * lazy 作成のため、 ユーザーに既存 cus_ が無いときだけ呼ぶ。
     */
    fun createCustomer(userId: String): String

    /**
     * カード保存用の SetupIntent を作成し、 その client_secret を返す。
     * [userId] は webhook で誰のカードか復元するため metadata に載せる。
     * client_secret はフロントへ渡し、 Stripe.js の confirmSetup でカード入力／3DS を行わせる。
     */
    fun createSetupIntent(
        stripeCustomerId: String,
        userId: String,
    ): String

    /** pm_ から表示用のカード情報（brand / 下 4 桁 / 有効期限）を取得する。 */
    fun retrievePaymentMethod(paymentMethodId: String): CardDetails

    /**
     * pm_ を Customer から切り離す（カード削除）。
     *
     * 冪等: 既に切り離し済み / 不在（恒久エラー）は成功扱いで握る。
     * 一時障害（ネットワーク断・レート制限・5xx）は例外を投げる —— 呼び出し側
     * （[jp.momiji.feature.command.payment.deletecard.CardDetacher] の pooledStreaming processor）の
     * 無限リトライに乗せて eventually に Stripe 側へ追従させるため。
     */
    fun detachPaymentMethod(paymentMethodId: String)

    /**
     * Stripe Customer（cus_）を削除する（ユーザー削除時の Stripe 側掃除）。
     *
     * 冪等性・例外の扱いは [detachPaymentMethod] と同じ: 恒久エラー（既に削除済み / 不在）は成功扱いで握り、
     * 一時障害は例外を投げて呼び出し側（StripeCustomerDeleter の pooledStreaming processor）の無限リトライに乗せる。
     */
    fun deleteCustomer(stripeCustomerId: String)

    /**
     * 注文の決済用 PaymentIntent を作成し、 client_secret と pi_ を返す。
     * - [amount] は円（JPY はゼロ十進通貨なので *100 しない）。
     * - [paymentMethodId] は確定済みの保存カード（pm_）。 confirm はフロントの Stripe.js で行う（オンセッション＝3DS をブラウザで）。
     * - [orderId] を metadata に載せ、 webhook（[PaymentWebhookParser]）で注文を相関する。
     *
     * 冪等: order ごとに固定の Idempotency-Key で、 リトライ・並走でも同一 PaymentIntent（= 同一 client_secret）を返す。
     */
    fun createPaymentIntent(
        stripeCustomerId: String,
        paymentMethodId: String,
        amount: Long,
        orderId: String,
    ): PaymentIntentResult

    /**
     * PaymentIntent を全額返金する（期限切れ後に課金成功してしまった場合のバックストップ）。
     *
     * 冪等・例外の扱いは [detachPaymentMethod] と同方針: 恒久エラー（既に返金済み等）は握り、 一時障害は投げる。
     */
    fun refundPayment(paymentIntentId: String)
}

data class CardDetails(
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
)

data class PaymentIntentResult(
    val clientSecret: String,
    val paymentIntentId: String,
)
