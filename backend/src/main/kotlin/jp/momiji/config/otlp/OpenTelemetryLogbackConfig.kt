package jp.momiji.config.otlp

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * OpenTelemetryのLogback Appenderをインストール
 * Lokiにログが転送されるよ！
 */
@Configuration
@Profile("observability-otlp")
class OpenTelemetryLogbackConfig(
    private val openTelemetry: OpenTelemetry,
) {
    @PostConstruct
    fun installAppender() {
        OpenTelemetryAppender.install(openTelemetry)
    }
}
