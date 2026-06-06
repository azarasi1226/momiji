package jp.momiji.feature.brand.archive

import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import jp.momiji.domain.Ulid
import jp.momiji.domain.ValidationException
import jp.momiji.feature.throwIfError
import jp.momiji.grpc.momiji.brand.archive.v1.ArchiveBrandRequest
import jp.momiji.grpc.momiji.brand.archive.v1.ArchiveBrandResponse
import jp.momiji.grpc.momiji.brand.archive.v1.ArchiveBrandServiceGrpcKt
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service

@Service
class ArchiveBrandGrpcService(
    private val commandGateway: CommandGateway,
) : ArchiveBrandServiceGrpcKt.ArchiveBrandServiceCoroutineImplBase() {
    override suspend fun archiveBrand(request: ArchiveBrandRequest): ArchiveBrandResponse {
        Ulid
            .validate(request.id)
            .onErr { error -> throw ValidationException(listOf(error)) }
            .onOk { id ->
                commandGateway
                    .archiveBrand(ArchiveBrandCommand(id = id))
                    .throwIfError()
            }

        return ArchiveBrandResponse.getDefaultInstance()
    }
}
