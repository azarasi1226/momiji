# ADR 0002: エラーの種類とGRPCの構造化エラー変換

- **ステータス**: 採用
- **作成日**: 2026-05-29
- **関連 ADR**: [0001](./0001-value-objects.md) (値オブジェクトの validation エラーが本 ADR の `ValidationError` 経路に流れる)

## コンテキスト

これまで gRPC のエラーは `Status.INVALID_ARGUMENT.withDescription(ex.message)` で文字列を返すだけだった:

- backend で `ValidationException(errors: List<ValidationError>)` を投げる
- gRPC が message を `"[name] 名前は必須です / [phoneNumber] 電話番号は..."` のような join 文字列にして返す
- frontend は string を表示するだけで、 「どの field のエラーか」 を識別できない

結果として、 form の field 別ハイライト等の UX 改善ができない状態だった。
加えて、 想定外例外時 (`else -> null`) は spring-grpc 既定の `UNKNOWN` 扱いで、 クライアントは原因究明できなかった。

## 決定

### 構造化エラー (proto details)

`grpc/momiji/common/v1/error.proto` に `ErrorDetail` メッセージを定義し、 `oneof` で型を表現:

```
- UseCaseError    → ビジネスルール違反 (例: ユーザー未存在、 メール重複)
- ValidationError → 値オブジェクト validation 集約エラー (field 別)
- UnknownError    → 想定外例外 (詳細は §安全な出し方 参照)
```

backend の `GrpcConfig.grpcExceptionHandler` で例外型を見て該当 `ErrorDetail` を `google.rpc.Status.details` に乗せる。 frontend は `ConnectError.findDetails(ErrorDetailSchema)` で型安全に取り出す。

### 各エラー型の使い分け

| 例外型 | gRPC Status | ErrorDetail oneof | フロント UI |
|---|---|---|---|
| `BusinessException` | `INVALID_ARGUMENT` | `useCaseError` | form 下部に赤字メッセージ |
| `ValidationException` | `INVALID_ARGUMENT` | `validationError` | field 別 border 赤 + 個別メッセージ |
| その他 (想定外) | `UNKNOWN` | `unknownError` | 「サーバーエラーが発生しました (問い合わせ番号: ...)」 |

### 新しいエラー型の追加パス

将来 `NotFoundError` / `ConflictError` 等を追加する場合:

1. `error.proto` の `oneof` に 1 行追加 + 専用 message を定義
2. `buf generate` で両側型生成
3. backend `grpcExceptionHandler` に `is XxxException ->` 1 枝追加
4. frontend `parseConnectError` に `case "xxxError"` 1 枝追加

## 妥協点と運用設計

### `UnknownError` の安全な出し方 (重要)

想定外例外の `ex.message` をそのままクライアントに返すと、 内部情報がブラウザの DevTools 経由で漏洩する:

| 想定外例外 | `.message` の中身 | 漏れる情報 |
|---|---|---|
| `JdbcSQLException` | `Column 'password_hash' not found` | 内部スキーマ |
| `RestClientException` | `Connection refused: cognito.internal.amazon.com:443` | 内部ホスト名 |
| `FileNotFoundException` | `/var/secrets/db.key not found` | ファイルパス |
| `NullPointerException` | `Cannot invoke User.email() because user is null` | コード構造 |

このため `UnknownError` は **固定メッセージ + correlationId (UUID)** だけを返し、 詳細はサーバーログにのみ出す:

```kotlin
val correlationId = UUID.randomUUID().toString()
logger.error(ex) { "予期せぬエラー correlationId=$correlationId" }
buildStatusException(
    Status.UNKNOWN,
    "サーバーエラーが発生しました",
    buildUnknownDetail(correlationId),
)
```

```proto
message UnknownError {
  string message = 1;       // 固定 "サーバーエラーが発生しました"
  string correlation_id = 2; // backend ログとの突合用 UUID
}
```

### 運用フロー

```
[ユーザー] 「サーバーエラーが発生しました (問い合わせ番号: 7f3a8e2c-...)」 を UI で見る
       ↓
[ユーザー] サポートに問い合わせ番号を伝える
       ↓
[サポート] backend ログを `grep 7f3a8e2c` で検索
       ↓
   "予期せぬエラー correlationId=7f3a8e2c-..." + 完全な stack trace がヒット
       ↓
[開発] 原因特定 → 修正
```

ユーザーには内部情報を見せず、 サポート診断性は維持される。 これが production 投入時の最低ライン。

## 帰結

良かった点:

- frontend が `findDetails(ErrorDetailSchema)` で型安全にエラーを扱える
- ValidationError 経由で field 別 UI (border 赤化、 個別メッセージ表示) ができるようになった
- 新エラー型の追加が `oneof` 1 行で済み、 拡張パスが明示的
- 想定外例外でも構造化されているので、 クライアントが「これは復旧不能か」 を識別できる
- `UnknownError` の固定メッセージ + correlationId 構造で、 情報漏洩なしでサポート診断可能

注意すべき点:

- `oneof` に case を追加するときは frontend 側の `parseConnectError` も同時に更新しないと「unhandled」 として fallback になる
- `UnknownError.message` を可変にすると情報漏洩経路が復活するので、 固定文字列で固定する規律が必要
- correlationId はサーバーログに必ず出すこと (出さないとサポート診断パスが切れる)

## 関連

- gRPC エラーモデル: https://grpc.io/docs/guides/error/
- google.rpc.Status (details): https://github.com/googleapis/googleapis/blob/master/google/rpc/status.proto
- ソースコード: `backend/src/main/kotlin/jp/momiji/config/grpc/GrpcConfig.kt`, `grpc/momiji/common/v1/error.proto`, `frontend/app/profile/actions.ts`
