package jp.momiji.feature.brand.archive

import jp.momiji.domain.brand.BrandStatus
import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandArchivedEvent
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class ArchiveBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: ArchiveBrandCommand,
        @InjectEntity state: State,
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

    @EventSourced(tagKey = MomijiEventTag.BRAND_ID, idType = String::class)
    class State(
        var status: BrandStatus?,
    ) {
        @EntityCreator
        constructor() : this(
            status = null,
        )

        @EventSourcingHandler
        fun evolve(event: BrandCreatedEvent) {
            status = BrandStatus.ACTIVE
        }

        @EventSourcingHandler
        fun evolve(event: BrandArchivedEvent) {
            status = BrandStatus.ARCHIVED
        }
    }
}
