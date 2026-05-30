package jp.momiji.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

/**
 * ユースケースのビジネスロジックを span 化する Aspect
 *
 * org.springframework.boot:spring-boot-starter-opentelemetry
 * net.ttddyy.observation:datasource-micrometer-spring-boot
 *
 * 上記２つのライブラリにより、 GRPCリクエスト、JDBCクエリはそれぞれ自動的に span 化され、Trace で追跡できるようになる。
 * しかし、自分たちで作ったビジネスロジックは span 化されないため、ユースケースの全体的な流れを trace で見ることができない。
 * この Aspect を使うと、 `jp.momiji.feature` 配下の public メソッドを全て span 化できる。
 * これにより、ユースケースのビジネスロジックも trace に乗るようになり、ユースケース全体の流れを trace で追跡できるようになる。
 */
@Aspect
@Component
class UseCaseLogicTracingAspect(
    private val observationRegistry: ObservationRegistry,
) {
    // `jp.momiji.feature` 配下の public メソッドを全て捕まえる pointcut。
    @Around("execution(public * jp.momiji.feature..*.*(..))")
    fun trace(joinPoint: ProceedingJoinPoint): Any? {
        // execution joinpoint なので signature は必ず MethodSignature。
        val signature = joinPoint.signature as MethodSignature
        // パラメータ型の simpleName を ( ) で囲んで付加することで、 オーバーロード ( 例: Axon の
        // `@EventHandler fun on(UserCreatedEvent)` と `on(ExternalIdentityLinkedEvent)` ) を span 名で区別する。
        val params = signature.parameterTypes.joinToString(", ") { it.simpleName }
        val spanName = "${signature.declaringType.simpleName}.${signature.name}($params)"
        return Observation
            .createNotStarted(spanName, observationRegistry)
            .observe<Any?> { joinPoint.proceed() }
    }
}
