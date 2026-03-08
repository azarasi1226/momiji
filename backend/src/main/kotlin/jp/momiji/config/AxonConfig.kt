package jp.momiji.config

import org.axonframework.conversion.Converter
import org.axonframework.messaging.core.unitofwork.transaction.jdbc.JdbcTransactionalExecutorProvider
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.GenericTokenTableFactory
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.JdbcTokenStore
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.JdbcTokenStoreConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class AxonConfig {
  // Axonの初期はJpaTokenStoreなので、JdbcTokenStoreに切り替えるための設定。
  @Bean
  fun tokenStore(
    dataSource: DataSource,
    converter: Converter,
  ): TokenStore {
    val tokenStore = JdbcTokenStore(
      JdbcTransactionalExecutorProvider(dataSource),
      converter,
      JdbcTokenStoreConfiguration.DEFAULT
    )
    // TokenStoreテーブルを作成する内部では IF NOT EXITSでテーブルが作成されてるので、存在しなかった場合のみ作成される。
    tokenStore.createSchema(
      // 汎用的なDBに対応したTableFactory
      GenericTokenTableFactory.INSTANCE,
    )

    return tokenStore
  }
}