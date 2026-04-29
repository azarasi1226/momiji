package jp.momiji.config

import jakarta.annotation.PostConstruct
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.GenericTokenTableFactory
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jdbc.JdbcTokenStore
import org.springframework.context.annotation.Configuration


@Configuration
class AxonConfig(
  // JPAを使用していない、かつJDBCを使っている場合は Axonが自動的にJdbcTokenStoreをDIに登録してくれる。
  // しかし、TokenStoreという抽象的なInterfaceで登録されているので、 init()の中でスマートキャストしている。
  private val tokenStore: TokenStore
) {
  @PostConstruct
  fun init() {
    if(tokenStore is JdbcTokenStore) {
      // TokenStoreテーブル(EventProcessorにおいて、どこまでEventが呼ばれたかを記録するテーブル)を作成する。
      // 内部では CREATE TABLE IF NOT EXITS構文によってテーブルが作成されてるので、Oracleなどでは使えないから自動的に呼ばれないのかな...?(自動的に呼んでほしい...)
      tokenStore.createSchema(
        GenericTokenTableFactory.INSTANCE,
      )
    }
  }
}