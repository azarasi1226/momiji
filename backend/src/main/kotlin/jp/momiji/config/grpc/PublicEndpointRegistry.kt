package jp.momiji.config.grpc

import io.grpc.BindableService
import org.springframework.stereotype.Component

/**
 * 起動時に全 gRPC サービス ([BindableService]) をスキャンし、
 * [PublicEndpoint] が付いたメソッドの "ServiceName/MethodName" を集めて保持する。
 *
 * [GrpcAuthInterceptor] がリクエストごとに `call.methodDescriptor.fullMethodName` を
 * このSetでチェックし、含まれていれば認証スキップする。
 */
@Component
class PublicEndpointRegistry(
    services: List<BindableService>,
) {
    val publicMethods: Set<String> =
        buildSet {
            services.forEach { service ->
                val serviceDef = service.bindService()
                serviceDef.methods.forEach { serverMethodDef ->
                    // 例: "jp.momiji.user.create.v1.CreateUserService/CreateUser"
                    val grpcFullMethodName = serverMethodDef.methodDescriptor.fullMethodName
                    // 例: "CreateUser"
                    val simpleMethodName = grpcFullMethodName.substringAfterLast('/')
                    // gRPC-Kotlin の規約で Kotlin側は先頭小文字: "createUser"
                    val kotlinMethodName = simpleMethodName.replaceFirstChar { it.lowercase() }

                    // 実装クラスにある Kotlin/Java method を引いて、@PublicEndpoint があれば登録
                    val javaMethod =
                        service::class.java.methods
                            .firstOrNull { it.name == kotlinMethodName }

                    if (javaMethod?.isAnnotationPresent(PublicEndpoint::class.java) == true) {
                        add(grpcFullMethodName)
                    }
                }
            }
        }
}
