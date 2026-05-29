package jp.momiji.domain

/**
 * 値オブジェクト検証エラーの基底。
 *
 * 各 sub-class は **対応する値オブジェクトの内部** (nested class/object) として定義する。
 * 例: `Name.Blank`, `Name.TooLong(100)`, `PhoneNumber.Invalid`, ...
 *
 * これにより:
 * - 「Name のバリデーションルール」 と 「Name 検証失敗時のメッセージ」 が同じファイルにある (高凝集)
 * - エラーを値オブジェクトの名前空間で参照できる (読みやすい)
 *
 * 注: あえて `sealed` にはしていない。 Kotlin の sealed 制約は「同 package」 で、
 * 値オブジェクトを `domain.user` 配下に置くこの構造では成立しない。
 * 実用上、 利用側 (`ValidationException` / gRPC 層) は `field` と `message` だけ参照していて
 * `when` の網羅性 check は使っていないので、 凝集度を優先して `abstract` にしている。
 *
 * - [field]: gRPC リクエストのフィールド名と一致させ、 クライアントがどこを直せばよいか判別できるようにする
 * - [message]: クライアント向けの日本語メッセージ
 */
abstract class DomainError(
    val field: String,
    val message: String,
)
