package jp.momiji.grpc

import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusException
import jp.momiji.feature.UseCaseException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.security.oauth2.jwt.JwtDecoder

@Configuration
class GrpcConfig {

  @Bean
  @Order(0)
  @GlobalServerInterceptor
  fun grpcAuthInterceptor(jwtDecoder: JwtDecoder): ServerInterceptor =
    GrpcAuthInterceptor(jwtDecoder)

  @Bean
  fun grpcExceptionHandler(): GrpcExceptionHandler = GrpcExceptionHandler { ex ->
    when (ex) {
      is UseCaseException -> StatusException(Status.INVALID_ARGUMENT.withDescription(ex.error.message))
      else -> null
    }
  }
}
