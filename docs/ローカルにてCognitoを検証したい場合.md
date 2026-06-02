# LocalでCognitoを検証したい場合

## Backend

### 1. CognitoConfig修正
```kotlin
@Configuration
@Profile("idp-cognito")
class CognitoConfig {
    @Bean
    fun cognitoIdentityProviderClient(
        @Value("\${momiji.cognito.region}") region: String,
    ): CognitoIdentityProviderClient =
        CognitoIdentityProviderClient
            .builder()
            .region(Region.of(region))
            // ここのプロファイル名を追加する。
            .credentialsProvider(DefaultCredentialsProvider.builder().profileName("dev_admin").build())
            .build()
}

```

### 2. application.yaml
ローカルプロファイルのkeyclaok.yamlをidp-cognitoに差し替える
```
      local:
        - app-common
        - app-local
        - observability-otlp
        - datastore-mysql
        - idp-cognito
        - mail-smtp
```


### 3. local.env.properties
環境変数に

`COGNITO_USER_POOL_ID={UserPoolID}`
を追加する

## FrontEnd

### 1. .env.local改修

```
// ここを設定
AUTH_PROVIDER=cognito

AUTH_KEYCLOAK_ID=momiji-frontend
AUTH_KEYCLOAK_SECRET=momiji-frontend-secret
AUTH_KEYCLOAK_ISSUER=http://localhost:8085/realms/momiji

// ここを設定
AUTH_COGNITO_ID=3l3fnp2mgu9lovfnvfe0e7iqqt
AUTH_COGNITO_SECRET=6h9fkchtb8lir56u10m8evo1tidnpjqcnjkfksboqsdop9gotot
AUTH_COGNITO_ISSUER=https://cognito-idp.ap-northeast-1.amazonaws.com/ap-northeast-1_dDATqYQIU
```
