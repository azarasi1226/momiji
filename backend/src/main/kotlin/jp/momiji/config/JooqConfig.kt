package jp.momiji.config;

import org.jooq.conf.RenderNameCase;
import org.jooq.impl.DefaultConfiguration
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {
    @Bean
    fun jooqCustomizer(): DefaultConfigurationCustomizer =
    DefaultConfigurationCustomizer { configuration: DefaultConfiguration ->
            configuration
                    .settings()
                    // MySQLはテーブル名やプロパティ名が小文字で定義されている場合に、大文字のクエリが発行されるとエラーになるのでSQLを小文字に変換する設定
                    .withRenderNameCase(RenderNameCase.LOWER)
    }
}
