package jp.momiji.feature.user.changeemail.request

import jp.momiji.events.user.EmailChangeRequested
import jp.momiji.feature.mail.MailSender
import jp.momiji.feature.user.changeemail.EmailChangePayload
import jp.momiji.feature.user.changeemail.EmailChangeTokenService
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EmailChangeEmailSender(
  private val emailChangeTokenService: EmailChangeTokenService,
  private val mailSender: MailSender,
  @Value("\${momiji.base-url:http://localhost:9090}") private val baseUrl: String,
) {
  @EventHandler
  fun on(event: EmailChangeRequested) {
    val token = emailChangeTokenService.sign(
      EmailChangePayload(userId = event.userId, newEmail = event.newEmail)
    )

    mailSender.send(
      to = event.newEmail,
      subject = "メールアドレス変更の確認",
      body = """
        メールアドレスの変更がリクエストされました。
        以下のトークンを入力してください

        $token

        このリクエストに心当たりがない場合は、このメールを無視してください。
      """.trimIndent(),
    )
  }
}
