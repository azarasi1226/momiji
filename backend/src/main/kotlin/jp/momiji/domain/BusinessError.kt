package jp.momiji.domain

/**
 * ビジネスルール違反を表すドメインエラー。
 *
 * 例: 「ユーザーが存在しません」 「メールアドレスが既に使用中です」 など、
 * use case 実行は通ったが ビジネス制約で reject された場合の表現。
 *
 * 値オブジェクト validation のエラー ([jp.momiji.domain.user.DomainError]) とは別軸で、
 * CommandHandler が `CommandResult.fail(BusinessError(...))` で返す。
 */
data class BusinessError(
    val message: String,
)
