# ADR github-0001: Issue テンプレートとラベルをコードで管理する

- **ステータス**: 採用（`.github/ISSUE_TEMPLATE/` に Issue Form 3 種 + `.github/labels.yml` + `label-sync` ワークフローを導入）
- **作成日**: 2026-06-17
- **関連**: [.github/ISSUE_TEMPLATE/](../../../.github/ISSUE_TEMPLATE/) / [.github/labels.yml](../../../.github/labels.yml) / [.github/workflows/label-sync.yml](../../../.github/workflows/label-sync.yml)

## コンテキスト

Issue を「プロダクトバックログ」として運用したい。そのために (1) 起票フォーマットを揃えて項目の抜け漏れを防ぎたい、(2) ラベルを GitHub 上で手作業管理せず、リポジトリのコードと同期させたい。

### 経緯（なぜ ADR にするか）

- Issue の書き方が人によってバラバラだと、バックログとしての一覧性・優先度付けが効かない。**入力項目とその順序を仕組みで強制**したい。
- ラベルを GitHub の Web UI で手作業管理すると、いつ・誰が・なぜ作ったかが残らず、環境間で揃わない。**定義をリポジトリに置いて唯一の正にしたい**。
- これらは backend のアーキテクチャ判断とは別レイヤー（リポジトリ運用）なので、ADR を `docs/adr/github/` に分けて記録する。

## 決定

### 1. Issue は GitHub Issue Form（YAML）で型を固定する

`.github/ISSUE_TEMPLATE/` に 3 種を置く。Markdown テンプレ（`.md`）ではなく Form（`.yml`）を採用し、必須項目を強制する。

- [feature_request.yml](../../../.github/ISSUE_TEMPLATE/feature_request.yml)（🚀 機能追加）
- [bug_report.yml](../../../.github/ISSUE_TEMPLATE/bug_report.yml)（🐛 バグ修正）
- [refactor.yml](../../../.github/ISSUE_TEMPLATE/refactor.yml)（🔧 リファクタリング）

### 2. 機能追加／リファクタの項目順を固定する

**概要(What) → 背景(なぜ/価値) → 対象外(スコープ境界) → 完了条件(AC) → タスク(How) → 依存**。

読み手の思考の流れ（何を→なぜ→どこまで→何が満たせたら完了→どう作る）に沿わせる。**完了条件（受入基準）はタスクより前**に置く。AC は「何をもって done か」というストーリーの定義であり、タスクはそれを満たすための手段の分解だから（タスクは AC から導く）。

- 「概要」は What に限定し、なぜ／目的は「背景」に書く（役割の重複を排除）。
- 「対象外(Non-goals)」枠でスコープ膨張を防ぐ。
- 「依存」は `Blocked by #41` の定型句で書き、GitHub が追跡・検索できる形にする。

### 3. ラベルは labels as code（定義ファイル + 同期ワークフロー）

[.github/labels.yml](../../../.github/labels.yml) を**唯一の正**とし、[label-sync.yml](../../../.github/workflows/label-sync.yml)（`crazy-max/ghaction-github-labeler`）が main への push（`labels.yml` 変更時）または手動実行で同期する。`gh label create` を手で叩かない。

### 4. 完全同期（`skip-delete: false`）にする

`labels.yml` に無いラベルは同期時に**削除**する。定義ファイルとリポジトリの状態を常に一致させ、「いつの間にか増えた／消えない」ラベルを無くす。残したいラベルは必ず `labels.yml` に書く。

現在の定義: `enhancement` / `bug` / `refactor` / `frontend` / `backend` / `infra` / `docs` / `dependencies`。

## なぜ必要か（この ADR の核心）

### Form で型を強制 = バックログの一覧性が保てる

自由記述の Markdown テンプレは「消さずに上書き」で項目が崩れやすい。Form は欄ごとに分かれ必須も効くので、起票時点で**最低限の情報（概要・完了条件・タスク）が揃う**ことを保証できる。

### AC をタスクより前に置く = アジャイルの定義順

完了条件（受入基準）が先にあると、タスクはそこから逆算して分解できる。順序が逆だと「作業を並べたが何が done か曖昧」な Issue になりやすい。

### labels as code = 環境・履歴・レビューが効く

ラベルの追加／変更が PR の diff に残り、レビューできる。手作業管理では失われる「なぜこのラベルがあるか」が `description` とコミット履歴に残る。

## 妥協点 / 検討した代替案

### 代替 A: Markdown テンプレ（`.md`）（不採用）

書き味は軽いが必須チェックが効かず、項目が崩れる。型の強制を優先して Form を採った。

### 代替 B: `skip-delete: true`（追加専用）（不採用、ただし安全側）

定義に無いラベルを消さないので事故りにくい。が「消えないラベルが残る」ため唯一の正にならない。管理を楽にする目的を優先して完全同期にした。

### 未導入: 選択肢→ラベルの自動付与 / PR パスラベラー

Issue の dropdown（frontend/backend 等）選択や、PR の変更パス（`actions/labeler`）からのラベル自動付与は有用だが今回は見送り。必要になれば追補する。

## 帰結

良かった点:

- 起票フォーマットが揃い、Issue が**そのままバックログ項目**になる
- ラベルの定義・変更が PR で追える（labels as code）

注意すべき点（重要）:

- **`skip-delete: false` なので、`labels.yml` に無いラベルは初回同期で消える**。GitHub デフォルトラベル（`good first issue` 等）や手動作成ラベルで残したいものは、事前に `labels.yml` へ追記すること。
- Issue テンプレの `labels:` が参照するラベルは**事前に存在が必要**（無いと黙って付かない）。テンプレで使うラベルは `labels.yml` にも定義しておく。
- ラベルを消す／リネームすると、過去 Issue/PR からそのラベルが外れる。

## 関連

- テンプレ: [.github/ISSUE_TEMPLATE/](../../../.github/ISSUE_TEMPLATE/)
- ラベル定義: [.github/labels.yml](../../../.github/labels.yml)
- 同期ワークフロー: [.github/workflows/label-sync.yml](../../../.github/workflows/label-sync.yml)
