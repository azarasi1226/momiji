package jp.momiji.feature.command.product.create

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.product.create.CreateProductCommand
import jp.momiji.feature.command.product.create.CreateProductGrpcService
import jp.momiji.grpc.momiji.product.create.createProductRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

class CreateProductGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val service = CreateProductGrpcService(commandGateway)

    private val validProductId = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
    private val validBrandId = "01BX5ZZKBKACTAV9WEVGEMMVRZ"

    @Test
    fun `正常系_期待した Command が CommandGateway に渡りIDを返す`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        val response =
            runBlocking {
                service.createProduct(
                    createProductRequest {
                        id = validProductId
                        brandId = validBrandId
                        name = "テスト商品"
                        description = "テスト説明"
                        imageUrl = "https://example.com/i.png"
                        price = 1000
                    },
                )
            }

        assertEquals(validProductId, response.id)
        verify(exactly = 1) {
            commandGateway.send(
                match<CreateProductCommand> {
                    it.id == validProductId &&
                        it.brandId == validBrandId &&
                        it.name.value == "テスト商品" &&
                        it.description.value == "テスト説明" &&
                        it.imageUrl?.value == "https://example.com/i.png" &&
                        it.price.value == 1000
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `正常系_image_url未指定なら imageUrl は null`() {
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        runBlocking {
            service.createProduct(
                createProductRequest {
                    id = validProductId
                    brandId = validBrandId
                    name = "テスト商品"
                    description = "テスト説明"
                    price = 1000
                },
            )
        }

        verify(exactly = 1) {
            commandGateway.send(
                match<CreateProductCommand> { it.imageUrl == null },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_不正フィールドは全エラーを蓄積し Command は流れない`() {
        val ex =
            assertThrows<ValidationException> {
                runBlocking {
                    service.createProduct(
                        createProductRequest {
                            id = "not-a-ulid"
                            brandId = validBrandId
                            name = ""
                            description = "テスト説明"
                            price = 0
                        },
                    )
                }
            }

        assertEquals(3, ex.errors.size)
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
