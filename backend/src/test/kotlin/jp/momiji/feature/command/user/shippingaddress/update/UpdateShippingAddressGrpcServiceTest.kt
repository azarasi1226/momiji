package jp.momiji.feature.command.user.shippingaddress.update

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.shippingaddress.update.v1.UpdateShippingAddressRequest
import jp.momiji.grpc.momiji.user.shippingaddress.update.v1.updateShippingAddressRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class UpdateShippingAddressGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = UpdateShippingAddressGrpcService(commandGateway, userIdResolver)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    private fun validRequest(builder: UpdateShippingAddressRequest.Builder.() -> Unit = {}): UpdateShippingAddressRequest =
        updateShippingAddressRequest {
            shippingAddressId = validUlid
            name = "受取 花子"
            phoneNumber = "080-9999-0000"
            postalCode = "220-0012"
            prefecture = "神奈川県"
            city = "横浜市西区"
            streetAddress = "みなとみらい4-5-6"
            building = ""
            deliveryNote = "インターホン不要"
        }.toBuilder().apply(builder).build()

    @Test
    fun `正常系_JWT解決したuserIdと検証済みVOで期待したCommandがCommandGatewayに渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        withAuth { runBlocking { service.updateShippingAddress(validRequest()) } }

        verify(exactly = 1) {
            commandGateway.send(
                match<UpdateShippingAddressCommand> {
                    it.userId == "test-user-id" &&
                        it.shippingAddressId == validUlid &&
                        it.name.value == "受取 花子" &&
                        it.prefecture.value == "神奈川県" &&
                        it.building.value == ""
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_不正なULIDならValidationExceptionでCommandは流れない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        assertThrows<ValidationException> {
            withAuth { runBlocking { service.updateShippingAddress(validRequest { shippingAddressId = "x" }) } }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `異常系_複数フィールドのエラーが集約される`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        val exception =
            assertThrows<ValidationException> {
                withAuth {
                    runBlocking {
                        service.updateShippingAddress(
                            validRequest {
                                postalCode = "2200012" // ハイフンなし
                                prefecture = "横浜県"
                            },
                        )
                    }
                }
            }

        val fields = exception.errors.map { it.field }
        assertTrue(fields.containsAll(listOf("postalCode", "prefecture")), "集約されたフィールド: $fields")
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
