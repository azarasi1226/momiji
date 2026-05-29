package jp.momiji.feature.mail

import org.springframework.context.annotation.Profile
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@Profile("mail-smtp")
class SmtpMailSender(
    private val javaMailSender: JavaMailSender,
) : MailSender {
    override fun send(
        to: String,
        subject: String,
        body: String,
    ) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = subject
        message.text = body
        javaMailSender.send(message)
    }
}
