package jp.momiji.feature.command.brand.archive

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.brand.BrandState
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class ArchiveBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: ArchiveBrandCommand,
        @InjectEntity state: BrandState,
        eventAppender: EventAppender,
    ): CommandResult {
        when (state.status) {
            null -> return ArchiveBrandCommandResult.brandNotFound()
            // 冪等性: すでにアーカイブ済みならイベントを出さず成功を返す
            BrandStatus.ARCHIVED -> return ArchiveBrandCommandResult.success()
            // アクティブならアーカイブ可能
            BrandStatus.ACTIVE -> {
                eventAppender.append(
                    BrandArchivedEvent(
                        id = command.id,
                    ),
                )
                return ArchiveBrandCommandResult.success()
            }
        }
    }
}
