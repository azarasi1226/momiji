package jp.momiji.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient

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
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build()
}
