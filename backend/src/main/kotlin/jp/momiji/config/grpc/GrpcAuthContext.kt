package jp.momiji.config.grpc

import io.grpc.Context
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

object GrpcAuthContext {
    val AUTH_KEY: Context.Key<JwtAuthenticationToken> = Context.key("auth")

    fun current(): JwtAuthenticationToken = AUTH_KEY.get() ?: throw StatusRuntimeException(Status.UNAUTHENTICATED)
}
