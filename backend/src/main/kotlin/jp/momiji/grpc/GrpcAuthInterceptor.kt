package jp.momiji.grpc

import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class GrpcAuthInterceptor(
  private val jwtDecoder: JwtDecoder,
) : ServerInterceptor {

  companion object {
    private val AUTHORIZATION_KEY: Metadata.Key<String> =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
  }

  override fun <ReqT, RespT> interceptCall(
    call: ServerCall<ReqT, RespT>,
    headers: Metadata,
    next: ServerCallHandler<ReqT, RespT>,
  ): ServerCall.Listener<ReqT> {
    // Hedearに認証JwtTokenが存在するか検証
    val authHeader = headers.get(AUTHORIZATION_KEY)
      ?: throw StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Authorization header is missing"))

    // JwtTokenを取り出し、IDPの公開鍵で検証
    val token = authHeader.removePrefix("Bearer ").trim()
    val jwt = try {
      jwtDecoder.decode(token)
    } catch (e: Exception) {
      throw StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Invalid token"))
    }

    // JwtTokenの検証に成功したらContextにTokenを埋め込む
    val authentication = JwtAuthenticationToken(jwt)
    val context = io.grpc.Context.current().withValue(GrpcAuthContext.AUTH_KEY, authentication)
    return Contexts.interceptCall(context, call, headers, next)
  }
}
