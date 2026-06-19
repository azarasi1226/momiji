package jp.momiji.feature.command.brand.create

import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.brand.BrandState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class CreateBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: CreateBrandCommand,
        @InjectEntity state: BrandState,
        eventAppender: EventAppender,
    ): CommandResult {
        // 冪等性: id は BFF が採番して渡す。 同じ id での再送 (リトライ) は
        // 新規イベントを出さず success を返す（ブランドの二重作成を防ぐ）。
        // status が non-null = 既に作成済み（ARCHIVED も含む）。
        if (state.status != null) {
            return CreateBrandCommandResult.success()
        }

        eventAppender.append(
            BrandCreatedEvent(
                id = command.id,
                name = command.name.value,
                description = command.description.value,
            ),
        )
        return CreateBrandCommandResult.success()
    }
}
