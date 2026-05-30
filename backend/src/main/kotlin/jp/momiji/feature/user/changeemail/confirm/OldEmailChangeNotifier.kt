package jp.momiji.feature.user.changeemail.confirm

import jp.momiji.event.user.EmailChangeConfirmedEvent
import jp.momiji.feature.mail.MailSender
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.springframework.stereotype.Component

/**
 * メールアドレス変更が確定したタイミングで、**旧メールアドレス** に変更通知を送る。
 *
 * セキュリティ上の意義：
 * - 攻撃者がアカウントを乗っ取って email を勝手に書き換えた場合、本来の所有者に気付かせる
 * - 「あなたのアドレスが {newEmail} に変更されました」と知れれば、サポート連絡で巻き戻し対応が可能
 *
 * 旧メールは [EmailChangeConfirmedEvent.previousEmail] に乗っているので、ここで Read DB を参照する必要はない。
 */
@Component
class OldEmailChangeNotifier(
    private val mailSender: MailSender,
) {
    @EventHandler
    fun on(event: EmailChangeConfirmedEvent) {
        mailSender.send(
            to = event.previousEmail,
            subject = "メールアドレスが変更されました",
            body =
                """
                あなたのアカウントのメールアドレスが、新しいアドレス
                ${event.email}
                に変更されました。

                この変更にお心当たりがない場合は、第三者がアカウントにアクセスしている可能性があります。
                すぐにサポートまでご連絡ください。
                """.trimIndent(),
        )
    }
}
