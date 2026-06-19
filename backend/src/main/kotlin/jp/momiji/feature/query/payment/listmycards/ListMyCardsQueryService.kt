package jp.momiji.feature.query.payment.listmycards

import iss.jooq.generated.tables.references.PAYMENT_METHODS
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class ListMyCardsQueryService(
    private val dsl: DSLContext,
) {
    fun findByUserId(userId: String): List<CardView> =
        dsl
            .select(
                PAYMENT_METHODS.ID,
                PAYMENT_METHODS.BRAND,
                PAYMENT_METHODS.LAST4,
                PAYMENT_METHODS.EXP_MONTH,
                PAYMENT_METHODS.EXP_YEAR,
                PAYMENT_METHODS.IS_DEFAULT,
                PAYMENT_METHODS.CREATED_AT,
            ).from(PAYMENT_METHODS)
            .where(PAYMENT_METHODS.USER_ID.eq(userId))
            // 登録順で固定する。 is_default でソートすると、 デフォルト変更のたびに行が入れ替わり、
            // 同じ見た目のカード（同番号のテストカード等）だと「バッジが常に1行目＝何も変わってない」ように
            // 見える錯覚を生む。 並びを安定させてバッジの移動が見えるようにする。
            .orderBy(PAYMENT_METHODS.CREATED_AT.asc())
            .fetch { record ->
                CardView(
                    id = record[PAYMENT_METHODS.ID]!!,
                    brand = record[PAYMENT_METHODS.BRAND]!!,
                    last4 = record[PAYMENT_METHODS.LAST4]!!,
                    expMonth = record[PAYMENT_METHODS.EXP_MONTH]!!,
                    expYear = record[PAYMENT_METHODS.EXP_YEAR]!!,
                    isDefault = record[PAYMENT_METHODS.IS_DEFAULT]!!,
                )
            }
}
