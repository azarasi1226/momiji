package jp.momiji.feature.command.product.discontinue

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.throwIfError
import jp.momiji.grpc.momiji.product.discontinue.DiscontinueProductRequest
import jp.momiji.grpc.momiji.product.discontinue.DiscontinueProductResponse
import jp.momiji.grpc.momiji.product.discontinue.DiscontinueProductServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class DiscontinueProductGrpcService(
    private val commandGateway: CommandGateway,
) : DiscontinueProductServiceGrpcKt.DiscontinueProductServiceCoroutineImplBase() {
    override suspend fun discontinueProduct(request: DiscontinueProductRequest): DiscontinueProductResponse {
        Ulid
            .validate(request.id)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { id ->
                commandGateway
                    .discontinueProduct(DiscontinueProductCommand(id = id))
                    .throwIfError()
            }

        return DiscontinueProductResponse.getDefaultInstance()
    }
}
