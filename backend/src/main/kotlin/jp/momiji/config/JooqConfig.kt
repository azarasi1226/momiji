package jp.momiji.config;

import org.jooq.conf.RenderNameCase;
import org.jooq.impl.DefaultConfiguration
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {
    // mysqlはテーブル名が小文字で定義されている際、大文字でクエリが発行されるとエラーになるので、クエリを小文字に変換するカスタマイザを定義
    @Bean
    fun jooqCustomizer(): DefaultConfigurationCustomizer =
    DefaultConfigurationCustomizer { configuration: DefaultConfiguration ->
            configuration
                    .settings()
                    .withRenderNameCase(RenderNameCase.LOWER)
    }
}
