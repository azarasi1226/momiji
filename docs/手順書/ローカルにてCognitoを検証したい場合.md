# LocalでCognitoを検証する方法

## Backend

### 1. CognitoConfigの仮修正

現状、Cognito は AWS 環境下でしか使われない想定なので `DefaultCredentialsProvider` にてクレデンシャルの解決をしている。
そのため、ローカルで利用する際は、ローカル環境に登録されている AWS Profile 名を指定するように差し替える必要がある。

#### 修正前

```:kotlin
    @Bean
    fun cognitoIdentityProviderClient(
        @Value("\${momiji.cognito.region}") region: String,
    ): CognitoIdentityProviderClient =
        CognitoIdentityProviderClient
            .builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build()
```

#### 修正後

```kotlin
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

### 2. application.yamlの仮修正

ローカルプロファイルの idp-keycloak を idp-cognito に差し替える。

#### 修正後

```yaml
      local:
        - app-common
        - app-local
        - observability-otlp
        - datastore-mysql
        - idp-keycloak
        - mail-smtp
        - storage-minio
```

#### 修正前

```yaml
      local:
        - app-common
        - app-local
        - observability-otlp
        - datastore-mysql
        # ここを差し替える
        - idp-cognito
        - mail-smtp
```

### 3. local.env.properties

idp-cognito.yaml が参照するため、環境変数に `COGNITO_USER_POOL_ID={UserPoolID}` を追加する

## Frontend

### 1. .env.localに以下の記述を追加する

```:text
// ここは変更
AUTH_PROVIDER=cognito

// ここは追加
COGNITO_CLIENT_ID=<????>
COGNITO_CLIENT_SECRET=<????>
COGNITO_ISSUER=<????>
```
