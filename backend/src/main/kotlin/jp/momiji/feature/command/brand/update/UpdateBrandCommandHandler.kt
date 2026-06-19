package jp.momiji.feature.command.brand.update

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.event.brand.BrandUpdatedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.brand.BrandState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class UpdateBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: UpdateBrandCommand,
        @InjectEntity state: BrandState,
        eventAppender: EventAppender,
    ): CommandResult {
        // 更新できるのは ACTIVE のときだけ。 未作成 (null) / アーカイブ済みは brandNotFound 扱い。
        if (state.status != BrandStatus.ACTIVE) {
            return UpdateBrandCommandResult.brandNotFound()
        }

        eventAppender.append(
            BrandUpdatedEvent(
                id = command.id,
                name = command.name.value,
                description = command.description.value,
            ),
        )
        return UpdateBrandCommandResult.success()
    }
}
