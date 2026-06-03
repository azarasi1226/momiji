package jp.momiji.infrastructure.mail

import jp.momiji.feature.mail.MailSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@Profile("mail-smtp")
class SmtpMailSender(
    private val javaMailSender: JavaMailSender,
    // From アドレスを明示しないと jakarta.mail が「can't determine local email address」で送信を拒否する。
    // 環境ごとに設定 (例: 本番 = noreply@momiji.jp、 ローカル = noreply@localhost)
    @Value("\${momiji.mail.from}") private val from: String,
) : MailSender {
    override fun send(
        to: String,
        subject: String,
        body: String,
    ) {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(to)
        message.subject = subject
        message.text = body
        javaMailSender.send(message)
    }
}
