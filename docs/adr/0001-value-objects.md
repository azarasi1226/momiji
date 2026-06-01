# ADR 0001: 値オブジェクトと Result 型の組み合わせによるバリデーションチェックの集約

- **ステータス**: 採用
- **作成日**: 2026-05-29

## コンテキスト

コレまでは値ブジェクとの検証に例外を採用しており、Commandを組み立てる際に一つでも不正なフィールドがあると即座に例外が投げられていた。

```kotlin
val command = UpdateUserCommand(
    id = userId,
    name = Name(request.name),                      // ← ここで throw するととAPIの利用者は Name の検証エラーしか気づけない
    phoneNumber = PhoneNumber(request.phoneNumber), // ← 次のフィールドのエラーに気づけない
    postalCode = PostalCode(request.postalCode),
    address1 = Address1(request.address1),
    address2 = Address2(request.address2),
)
```

この方式だと、 name と phoneNumber の両方が不正でも **最初に評価された name の例外しかクライアントに返せない**。 ユーザーは「name を直して再送 → 今度は phoneNumber で弾かれる」 という往復を強いられる。 また `IllegalArgumentException` を gRPC レイヤで握り潰してメッセージを組み立て直す必要があり、 フィールド名 (どの項目が NG か) が message 文字列からしか復元できない。

コレを解決するために、Result型を導入し、 **すべてのフィールドの検証結果を集約してクライアントに返す** 方式に切り替える。 これにより、 クライアントは「name と phoneNumber の両方が不正」 という状態を一度のリクエストで知ることができ、 UX が大幅に向上する。



## 決定

### 配置

```
domain/                         ← ドメイン概念 (vertical slice に依存しない)
├── DomainError.kt              ← abstract: field + message を持つ
├── BusinessError.kt            ← ビジネスルール違反 (例: ユーザー未存在)
├── UseCaseException.kt         ← BusinessError ラップ例外
├── ValidationException.kt      ← List<DomainError> ラップ例外
└── user/                       ← User コンテキストの値オブジェクト
    ├── Name.kt
    ├── Email.kt
    ├── PhoneNumber.kt
    ├── PostalCode.kt
    ├── Address1.kt
    ├── Address2.kt
    ├── EmailChangeToken.kt      ← JWT 風 3 セグメント形式の確認用トークン
    └── (User コンテキストの値オブジェクト)
```

`feature/` (use case 層) は `domain/` に一方向依存。 逆方向の依存は無い。

### 値オブジェクトのテンプレート

```kotlin
data class Name internal constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 100

        fun create(input: String): Result<Name, DomainError> {
            if (input.isBlank()) return Err(Blank)
            if (input.length > MAX_LENGTH) return Err(TooLong)
            return Ok(Name(input))
        }

        object Blank : DomainError("name", "名前は必須です")
        object TooLong : DomainError("name", "名前は $MAX_LENGTH 文字以内で入力してください")
    }
}
```

ポイント:

- `internal constructor` + `create()` で「外部から validation を bypass して構築」 を禁止
- エラーは値オブジェクト内の `companion object` に nested で定義 → 凝集度を最大化
- メッセージ内に `$MAX_LENGTH` を埋め込んで定数との同期を保つ
- ライブラリ: [kotlin-result](https://github.com/michaelbull/kotlin-result) の `Result<V, E>` を採用

### 検証集約

gRPC 層 (Command 組み立て地点) で `zipOrAccumulate` を使い、 **全エラーを蓄積** してクライアントに返す:

```kotlin
val commandResult = zipOrAccumulate(
    { Name.create(request.name) },
    { PhoneNumber.create(request.phoneNumber) },
    { PostalCode.create(request.postalCode) },
    { Address1.create(request.address1) },
    { Address2.create(request.address2) },
) { name, phoneNumber, postalCode, address1, address2 ->
    UpdateUserCommand(id = userId, name, phoneNumber, postalCode, address1, address2)
}

commandResult
    .onFailure { errors -> throw ValidationException(errors) }
    .onSuccess { command -> commandGateway.updateUser(command).throwIfError() }
```

単一フィールドの場合は `getOrElse { throw ValidationException(listOf(it)) }` でシンプルに。

`ValidationException` は `GrpcConfig.grpcExceptionHandler` で `Status.INVALID_ARGUMENT` にマッピングされる。

## 妥協点 (ここを質問されたら全部この ADR を見せる)

### 1. なぜ `private constructor` ではなく `internal` か

Axon が Command を AxonServer に送る際に **Jackson で byte[] serde** する。 Jackson の reflection は JVM 可視性を要求するため `private constructor` だと `Cannot construct instance` で deserialize 失敗。

- Kotlin `private` (class member) → JVM `private` → Jackson 不可
- Kotlin `internal` → JVM `public` (name mangling) → Jackson OK

完全 `private` を維持したい場合は Custom Jackson Deserializer を 1 個ずつ書く必要があり、 コスト vs 防御強度が見合わない。 `internal` + 後述の `-Xconsistent-data-class-copy-visibility` で「外部モジュールからの bypass を完全禁止」 まで届くので実用的妥協点とする。

### 2. なぜ `-Xconsistent-data-class-copy-visibility` を追加したか

Kotlin の `data class` は primary constructor が `internal` でも、 自動生成される `copy()` がデフォルト `public`。 つまり:

```kotlin
val n = Name.create("Alice").get()!!
val sneaky = n.copy(value = "")  // ← validation を bypass
```

これを防ぐため compiler flag で「copy() の可視性を primary constructor に揃える」。 将来 Kotlin の default 挙動になる予定の先取り。

### 3. なぜ `DomainError` は `sealed` ではなく `abstract` か

Kotlin の `sealed` 制約は「sub-class は **同 package** 内」。 `DomainError` を `domain/` 直下に置き、 sub-class を `domain.user.Name` 等の中に nested で置く構造ではこの制約を満たせない。

`when` の網羅性 check を意図的に捨てる代わりに、 **凝集度** (Name のルールは Name.kt 1 ファイルで完結) を優先した。 利用側 (`ValidationException`) は `field` と `message` を join するだけで `is` 分岐していないので、 実用上の影響なし。

### 4. なぜ Event はあえて String のまま

Command は値オブジェクト型を持つが、 Event (例: `UserUpdatedEvent`) は String のまま。 Event Sourcing では event store にスキーマ進化リスクがあり、 「過去 event を将来の値オブジェクト定義で復元できなくなる」 のを避けるため、 event の field は **プリミティブで保つ** のがベストプラクティス。

CommandHandler 内で `command.name.value` で平文を取り出して event に積む。

### 7. なぜ Address1 + Address2 を 1 つの `Address` にまとめないか

「住所」 を `Address(line1, line2)` の 1 つの値オブジェクトにまとめると DDD としては綺麗だが、 以下の技術制約がある:

- `Address.create(line1, line2)` が **複数フィールドの集約 validation** を内包する場合、 戻り値は `Result<Address, List<DomainError>>`
- 他の値オブジェクトは `Result<T, DomainError>` (単一エラー)
- `zipOrAccumulate` は **E 型を統一** する必要があるため、 上記の混在は型レベルで合成不可

回避策として gRPC 層で `buildList` による自前集約に切り替える方法もあるが、 `zipOrAccumulate` の優雅さを失う trade-off があり、 当面は `Address1` / `Address2` の分割を維持する。

### 6. 全 Command の型化状況

| Command | 型化フィールド | 備考 |
|---|---|---|
| `UpdateUserCommand` | name / phoneNumber / postalCode / address1 / address2 | `zipOrAccumulate` で 5 フィールド集約 |
| `RequestEmailChangeCommand` | newEmail (Email 型) | 単一フィールドなので `getOrElse` |
| `CreateUserCommand` | email (Email 型) / oidcIdentityProvider (IdentityProvider enum) | IDP 経由 (信頼境界内) だが broken IDP への防御。 enum は `domain/idp/` に集約、 文字列→enum 変換は各 IDP 実装側 (`KeycloakUserClient`, `CognitoUserClient`) で行い、 ドメイン層に IDP 固有の文字列フォーマットを抱え込ませない |
| `ConfirmEmailChangeCommand` | token (EmailChangeToken 型) | 形式チェックを値オブジェクトに、 署名/期限検証は `EmailChangeTokenService` |
| `DeleteUserCommand` | (id のみ、 型化対象なし) | 内部 userId は信頼境界 |

`Address1` / `Address2` を 1 つの `Address(line1, line2)` にまとめる案は検討したが見送り (理由は §7)。

## 帰結

良かった点:

- CommandHandler に渡る時点で全フィールドが validation 済みである保証が型レベルで得られる
- フィールド別エラーが全部集約されて 1 レスポンスで返るので UX が向上
- 凝集度の高い構造 (Name のルール変更は Name.kt 1 ファイル) で保守性が上がる

注意すべき点:

- 値オブジェクト追加のたびに data class + companion + error + test を書く必要があり、 ある程度のテンプレート的繰り返しは避けられない
- `internal constructor` の妥協点を理解せず `private` にすると Axon serde で落ちる
- Event を値オブジェクト型化すると将来痛い目を見る (上述)

## 関連

- [kotlin-result 公式](https://github.com/michaelbull/kotlin-result)
- [Kotlin: data class consistent copy visibility](https://kotlinlang.org/docs/whatsnew21.html#improved-handling-of-non-public-members-in-data-classes)
- [ADR 0002](./0002-grpc-error-response.md): 値オブジェクトの `ValidationException` がどう gRPC 構造化エラーに変換されるか
- ソースコード: `backend/src/main/kotlin/jp/momiji/domain/`
