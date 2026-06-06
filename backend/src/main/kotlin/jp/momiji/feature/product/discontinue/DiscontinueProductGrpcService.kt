package jp.momiji.feature.product.discontinue

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.momiji.product.discontinue.v1.DiscontinueProductRequest
import jp.momiji.grpc.momiji.product.discontinue.v1.DiscontinueProductResponse
import jp.momiji.grpc.momiji.product.discontinue.v1.DiscontinueProductServiceGrpcKt
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
