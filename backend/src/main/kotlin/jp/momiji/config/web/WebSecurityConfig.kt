package jp.momiji.config.web

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Stripe webhook 用の HTTP セキュリティ設定。
 *
 * spring-boot-starter-web を入れたことで oauth2-resource-server が HTTP を自動 secure するが、
 * Stripe は JWT を送れない（真正性は署名検証で担保する）。 そのため webhook 配下のパスだけを
 * 切り出して permitAll ＋ CSRF 無効にする。 securityMatcher で限定したチェインを置くことで、
 * 他パス（将来の HTTP エンドポイント）の既定セキュリティには影響しない。
 */
@Configuration
@Profile("payment-stripe")
class WebSecurityConfig {
    @Bean
    fun webhookSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/webhooks/**")
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .csrf { it.disable() }
        return http.build()
    }
}
