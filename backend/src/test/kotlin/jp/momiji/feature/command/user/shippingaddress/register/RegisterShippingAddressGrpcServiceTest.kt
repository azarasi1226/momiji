package jp.momiji.feature.command.user.shippingaddress.register

import io.grpc.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jp.momiji.config.grpc.GrpcAuthContext
import jp.momiji.domain.ValidationException
import jp.momiji.feature.command.CommandResult
import jp.momiji.feature.command.UserIdResolver
import jp.momiji.grpc.momiji.user.shippingaddress.register.v1.RegisterShippingAddressRequest
import jp.momiji.grpc.momiji.user.shippingaddress.register.v1.registerShippingAddressRequest
import kotlinx.coroutines.runBlocking
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.concurrent.CompletableFuture

class RegisterShippingAddressGrpcServiceTest {
    private val commandGateway = mockk<CommandGateway>()
    private val userIdResolver = mockk<UserIdResolver>()
    private val service = RegisterShippingAddressGrpcService(commandGateway, userIdResolver)

    private val validUlid = "01ARZ3NDEKTSV4RRFFQ69G5FAV"
    private val mockAccessToken = mockk<Jwt>()
    private val mockJwt = mockk<JwtAuthenticationToken> { every { token } returns mockAccessToken }

    private fun <T> withAuth(block: () -> T): T =
        Context
            .current()
            .withValue(GrpcAuthContext.AUTH_KEY, mockJwt)
            .call(block)

    private fun validRequest(builder: RegisterShippingAddressRequest.Builder.() -> Unit = {}): RegisterShippingAddressRequest =
        registerShippingAddressRequest {
            id = validUlid
            name = "受取 太郎"
            phoneNumber = "090-1234-5678"
            postalCode = "150-0041"
            prefecture = "東京都"
            city = "渋谷区"
            streetAddress = "神南1-2-3"
            building = "momijiビル 4F"
            deliveryNote = "置き配可"
            makeDefault = true
        }.toBuilder().apply(builder).build()

    @Test
    fun `正常系_JWT解決したuserIdと検証済みVOで期待したCommandがCommandGatewayに渡る`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"
        every { commandGateway.send(any(), CommandResult::class.java) } returns
            CompletableFuture.completedFuture(CommandResult.success())

        withAuth { runBlocking { service.registerShippingAddress(validRequest()) } }

        verify(exactly = 1) {
            commandGateway.send(
                match<RegisterShippingAddressCommand> {
                    it.userId == "test-user-id" &&
                        it.id == validUlid &&
                        it.name.value == "受取 太郎" &&
                        it.phoneNumber.value == "090-1234-5678" &&
                        it.postalCode.value == "150-0041" &&
                        it.prefecture.value == "東京都" &&
                        it.city.value == "渋谷区" &&
                        it.streetAddress.value == "神南1-2-3" &&
                        it.building.value == "momijiビル 4F" &&
                        it.deliveryNote.value == "置き配可" &&
                        it.makeDefault
                },
                CommandResult::class.java,
            )
        }
    }

    @Test
    fun `異常系_不正なULIDならValidationExceptionでCommandは流れない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        assertThrows<ValidationException> {
            withAuth { runBlocking { service.registerShippingAddress(validRequest { id = "not-a-ulid" }) } }
        }

        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }

    @Test
    fun `異常系_実在しない都道府県ならValidationExceptionでCommandは流れない`() {
        every { userIdResolver.resolve(mockAccessToken) } returns "test-user-id"

        assertThrows<ValidationException> {
            withAuth { runBlocking { service.registerShippingAddress(validRequest { prefecture = "東京県" }) } }
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
                        service.registerShippingAddress(
                            validRequest {
                                name = ""
                                phoneNumber = "09012345678" // ハイフンなし
                                city = ""
                            },
                        )
                    }
                }
            }

        val fields = exception.errors.map { it.field }
        assertTrue(fields.containsAll(listOf("name", "phoneNumber", "city")), "集約されたフィールド: $fields")
        verify(exactly = 0) { commandGateway.send(any(), CommandResult::class.java) }
    }
}
