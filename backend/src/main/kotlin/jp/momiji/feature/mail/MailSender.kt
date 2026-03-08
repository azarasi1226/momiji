package jp.momiji.feature.mail

interface MailSender {
  fun send(to: String, subject: String, body: String)
}
