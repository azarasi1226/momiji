package jp.momiji.config

import org.jooq.conf.RenderNameCase
import org.jooq.impl.DefaultConfiguration
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqConfig {
    @Bean
    fun jooqCustomizer(): DefaultConfigurationCustomizer =
        DefaultConfigurationCustomizer { configuration: DefaultConfiguration ->
            configuration
                .settings()
                // jOOQ codegen は識別子を大文字（IS_DEFAULT 等）で生成し、 既定で quote するため "IS_DEFAULT" と発行する。
                // PostgreSQL は unquoted 識別子を小文字に畳むため列は is_default で作られ、 大文字 quote だと不一致でエラーになる。
                // レンダリング名を小文字に変換して列名（小文字）と一致させる。
                .withRenderNameCase(RenderNameCase.LOWER)
        }
}
