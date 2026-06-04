package jp.momiji.port.mail

interface MailSender {
    fun send(
        to: String,
        subject: String,
        body: String,
    )
}
