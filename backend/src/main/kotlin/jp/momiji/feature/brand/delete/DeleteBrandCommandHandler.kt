package jp.momiji.feature.brand.delete

import jp.momiji.event.MomijiEventTag
import jp.momiji.event.brand.BrandCreatedEvent
import jp.momiji.event.brand.BrandDeletedEvent
import jp.momiji.feature.CommandResult
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.springframework.stereotype.Component

@Component
class DeleteBrandCommandHandler {
    @CommandHandler
    fun handle(
        command: DeleteBrandCommand,
        @InjectEntity state: State,
        eventAppender: EventAppender,
    ): CommandResult {
        if (!state.created) {
            return DeleteBrandCommandResult.brandNotFound()
        }

        // 冪等性: すでに削除済みならイベントを出さず成功を返す
        if (state.deleted) {
            return DeleteBrandCommandResult.success()
        }

        // TODO(Product 実装後): ブランドに紐づく商品が残っている場合は削除を拒否する。
        //   inaba では State が ProductCreated/Deleted を購読して productIds を持ち、
        //   isNotEmpty なら hasLinkedProducts エラーにしている。 Product feature 追加時に同様のガードを足す。

        eventAppender.append(
            BrandDeletedEvent(
                id = command.id,
            ),
        )
        return DeleteBrandCommandResult.success()
    }

    @EventSourced(tagKey = MomijiEventTag.BRAND_ID, idType = String::class)
    class State(
        var created: Boolean,
        var deleted: Boolean,
    ) {
        @EntityCreator
        constructor() : this(
            created = false,
            deleted = false,
        )

        @EventSourcingHandler
        fun evolve(event: BrandCreatedEvent) {
            created = true
        }

        @EventSourcingHandler
        fun evolve(event: BrandDeletedEvent) {
            deleted = true
        }
    }
}
