package jp.momiji.feature.command.brand

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced

/**
 * brand_id 境界の共有 DCB State。 ブランドのライフサイクル状態（[status]）だけを見る。
 * create / update / archive の各 CommandHandler が共有する
 * （横断の共有 State は domain フォルダ直下に置く。 [jp.momiji.feature.command.order.OrderState] と同じ方針）。
 *
 * - create: 既に存在するか（`status != null`、 ARCHIVED 含む）の冪等判定
 * - update: ACTIVE のときだけ更新（それ以外は brandNotFound）
 * - archive: null=不在 / ARCHIVED=冪等 no-op / ACTIVE=アーカイブ
 */
@EventSourced(tagKey = MomijiEventTag.BRAND_ID, idType = String::class)
class BrandState(
    var status: BrandStatus?,
) {
    @EntityCreator
    constructor() : this(status = null)

    @EventSourcingHandler
    fun evolve(event: BrandCreatedEvent) {
        status = BrandStatus.ACTIVE
    }

    @EventSourcingHandler
    fun evolve(event: BrandArchivedEvent) {
        status = BrandStatus.ARCHIVED
    }
}
