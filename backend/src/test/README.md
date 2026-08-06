# テスト方針

## 実行

```bash
# Java 25 必須。 統合テストは Testcontainers を使うので Docker 起動が前提。
./gradlew test                       # 全テスト
./gradlew test --tests "*XxxTest"    # 個別
```

## テスト対象

パッケージは `jp.momiji.` 配下。テストは本体と同じ階層にミラーで置く。

| 対象 | 重要度 | テスト手法 | 種別 | パッケージ | 例 |
|---|---|---|---|---|---|
| 値オブジェクト・ドメインロジック | 高 | 出力値ベース | 単体テスト | `domain/**` | `EmailTest` |
| CommandHandler | 高 | 出力値ベース | 統合テスト | `feature/command/**` | `CreateUserCommandHandlerTest` |
| EventHandler 等（外部依存トリガー）| 高 | コミュニケーションベース | 統合テスト | `feature/command/**` | `IdpUserDeleterTest` |
| GrpcService（Command系）| 中 | コミュニケーションベース | 単体テスト（モック）| `feature/command/**`（`*GrpcService`）| `CreateUserGrpcServiceTest` |
| 共通ロジック（純粋関数）| 小 | 出力値ベース | 単体テスト | `util` / `feature/**` 等 | `PagingTest` |

### テスト手法について

2 つを使い分ける（表の「テスト手法」列）。

- **出力値ベース**: 入力に対する戻り値・発行イベントを検証する。
- **コミュニケーションベース**: 依存を正しく呼んだか、また異常系（validation・認証失敗など）では呼ばないかを `verify` で検証する。

### 統合テストのやり方

`MomijiIntegrationTestBase` を継承し `given().events(...).when().command(...).then()` で検証（PostgreSQL + Axon Server コンテナ）。

- テスト間の独立性: read DB は `@BeforeEach` で毎回全削除する。EventStore はリセットできないので、テストごとに別の id を使って干渉を避ける。
- モック（`@MockkBean`）は各テストではなく基底クラスにまとめて宣言する。テストごとにモックの組み合わせが変わると Spring が「別の設定」とみなして ApplicationContext を作り直し、その際に Axon の起動ハンドラが二重実行され `RepositoryAlreadyRegisteredException` で落ちるため。

## 非テスト対象

テストしても旨味が薄い / コストに見合わないもの。

- **DTO・ただのデータクラス**: ロジックが無く、テストしても旨味がない。
- **Query 系**: ビジネスロジックとは言えず、表示要件で頻繁に変わるため退行（テストが壊れる）が起きやすい。旨味が少ない。view→proto の写経マッピングもここに含む。
- **Config・ビジネスロジックの無い処理**: Bean 配線や設定など。
- **外部依存そのものの処理**: 実際の外部呼び出し（IdP / SMTP / 決済 / ストレージ）。自動テストではモックで代替し、実疎通は別途（手動または本番相当環境）で確認する。
- **自動生成コード**: proto(gRPC) や jOOQ などの自動生成コード。
