package jp.momiji.feature.command.payment.preparecard

import iss.jooq.generated.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

/**
 * users ReadModel から既存の Stripe Customer ID（cus_）を読む。
 * GrpcService が「既存 cus_ を使うか新規作成するか」を決める lazy Customer 判定用。
 *
 * NOTE: この列を更新する projection（jp.momiji.projection.payment）は pooledStreaming（**非同期**）なので、
 * 初回 prepare の直後に再度呼ばれると未反映の null を読みうる。 その場合も二重作成にはならない:
 * - Stripe 側: createCustomer の Idempotency-Key（userId 固定）により同一 Customer に解決される
 * - イベント側: PrepareCardRegistrationCommandHandler が記録済みなら冪等に no-op
 */
@Component
class StripeCustomerReader(
    private val dsl: DSLContext,
) {
    fun findByUserId(userId: String): String? =
        dsl
            .select(USERS.STRIPE_CUSTOMER_ID)
            .from(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOne(USERS.STRIPE_CUSTOMER_ID)
}
