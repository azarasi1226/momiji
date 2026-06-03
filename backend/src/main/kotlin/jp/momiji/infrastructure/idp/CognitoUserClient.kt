package jp.momiji.infrastructure.idp

import io.github.oshai.kotlinlogging.KotlinLogging
import jp.momiji.feature.idp.IdpUserClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException

private val logger = KotlinLogging.logger {}

@Component
@Profile("idp-cognito")
class CognitoUserClient(
    @Value("\${momiji.cognito.user-pool-id}") private val userPoolId: String,
    private val cognitoClient: CognitoIdentityProviderClient,
) : IdpUserClient {
    override fun updateEmail(
        oidcSubject: String,
        newEmail: String,
    ) {
        try {
            cognitoClient.adminUpdateUserAttributes(
                AdminUpdateUserAttributesRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(oidcSubject)
                    .userAttributes(
                        AttributeType
                            .builder()
                            .name("email")
                            .value(newEmail)
                            .build(),
                        // 本システムでメールアドレス検証は済んでいるため、 email_verified も一緒に更新してtrueにする。
                        // これをやらないとCognito側ではメールアドレスが変更時に、自動的にemail_verified = falseとなってしまう。
                        AttributeType
                            .builder()
                            .name("email_verified")
                            .value("true")
                            .build(),
                    ).build(),
            )
        } catch (e: UserNotFoundException) {
            // ユーザーすでに削除されているということは２回叩かれた可能性が高いので冪等性を保つために例外は握りつぶし、ログだけ出す
            logger.error { "Cognitoユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Cognitoユーザーのメールアドレスを更新しました: oidcSubject=$oidcSubject" }
    }

    override fun deleteUser(oidcSubject: String) {
        try {
            cognitoClient.adminDeleteUser(
                AdminDeleteUserRequest
                    .builder()
                    .userPoolId(userPoolId)
                    .username(oidcSubject)
                    .build(),
            )
        } catch (e: UserNotFoundException) {
            // ユーザーすでに削除されているということは２回叩かれた可能性が高いので冪等性を保つために例外は握りつぶし、ログだけ出す
            logger.error { "Cognitoユーザーが見つかりません: oidcSubject=$oidcSubject" }
            return
        }

        logger.info { "Cognitoユーザーを削除しました: oidcSubject=$oidcSubject" }
    }
}
