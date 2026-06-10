# Dependabot の自動マージとレビュー必須化（GitHub 設定）

Dependabot の **patch 更新だけ**を自動マージしつつ、それ以外の PR は**レビュー必須**にするための GitHub リポジトリ設定をまとめる。
ワークフロー実体は [`.github/workflows/dependabot-auto-merge.yaml`](../.github/workflows/dependabot-auto-merge.yaml)。

## 実現したい運用

| 種類 | 挙動 |
| --- | --- |
| Dependabot の **patch** 更新（`x.y.Z`） | ワークフローが `github-actions[bot]` で承認 → required check 通過後に**自動マージ** |
| Dependabot の **minor / major** 更新 | ワークフローは承認しない → **人間のレビュー必須**（通常 PR と同じ） |
| 通常の PR | **人間のレビュー必須** |

ポイントは「**承認されないとマージできない**」を全 PR に課したうえで、patch だけ bot 承認で例外的に通す、という構成。
ワークフロー側は `if: ...version-update:semver-patch` で patch のみに限定しているため、minor/major は承認されず止まる。

## 必要な GitHub 設定（3つ）

Web UI 側の操作。リポジトリ管理者権限が必要。**3つが3つとも別メニュー**にあるので注意（特に① は「Branches/Rulesets」ではなく **Actions** 配下）。

| 設定 | 場所（Settings 配下） | 役割 |
| --- | --- | --- |
| ① Allow GitHub Actions to create and approve pull requests | **Actions → General → Workflow permissions** | bot が approve できるようにする |
| ② Allow auto-merge | **General → Pull Requests** | `gh pr merge --auto` を使えるようにする |
| ③ Require a pull request / approvals=1 / status checks | **Branches（Rulesets）** | 承認必須・CI必須のルール |

### ① bot による承認を許可する

**場所**: `Settings → Actions → General` → ページ最下部の **Workflow permissions** セクション
直リンク: `https://github.com/<owner>/<repo>/settings/actions`

- ☑ **Allow GitHub Actions to create and approve pull requests**
  （日本語 UI: 「GitHub Actions によるプルリクエストの作成と承認を許可する」）

> ⚠️ approve の設定なのに **Branches/Rulesets ではなく Actions 配下**にある。ここが一番ハマりやすい。

これが OFF だと、ワークフローの `gh pr review --approve` が次のエラーで落ちる:

```text
failed to create review: GitHub Actions is not permitted to approve pull requests. (addPullRequestReview)
```

> `GITHUB_TOKEN`（= `github-actions[bot]`）は、この設定が ON のときだけ PR を承認できる。

このチェックボックスの状態は API の `can_approve_pull_request_reviews` と一致する。CLI で確認/設定も可能:

```bash
# 現在値を確認（false=OFF / true=ON）
gh api repos/<owner>/<repo>/actions/permissions/workflow

# ON にする
gh api -X PUT repos/<owner>/<repo>/actions/permissions/workflow \
  -F default_workflow_permissions=read \
  -F can_approve_pull_request_reviews=true
```

### ② auto-merge を許可する

`Settings → General → Pull Requests`

- ☑ **Allow auto-merge**

これが OFF だと、ワークフローの `gh pr merge --auto` が `auto-merge is not allowed for this repository` で落ちる。
`--auto` は「他の required check が全部成功したら自動的にマージ」を予約する機能で、この設定が前提。

### ③ レビュー必須の branch protection を追加する

`Settings → Branches`（または `Settings → Rules → Rulesets`）で `main` に対してルールを追加:

- ☑ **Require a pull request before merging**
  - ☑ **Require approvals** → 承認数 **1**
- ☑ **Require status checks to pass before merging**
  - 必須チェックに **Pull Request Verification** の各ジョブ（`proto-lint` / `backend` / `frontend`）を指定

> status check を必須にしておくことで、`gh pr merge --auto` が「CI 緑になるまで待ってからマージ」してくれる。
> required check を 1 つも指定しないと、auto-merge は承認直後に即マージしてしまい CI を待たない。

## 動作の流れ（patch 更新の場合）

```text
Dependabot が patch PR を作成
  └─ Pull Request Verification（proto-lint / backend / frontend）が走る
  └─ dependabot-auto-merge が走る
       ├─ fetch-metadata で update-type を判定（= semver-patch）
       ├─ gh pr review --approve   … 設定① で許可された bot 承認 → required review(1) を満たす
       └─ gh pr merge --auto       … 設定② で予約。required check が全部緑になったら自動マージ
```

minor / major PR では `if` 条件に外れるため approve も merge も実行されず、人間が承認するまで止まる。

## 注意点（branch protection の追加オプション）

required review には bot 承認と相性の悪いオプションがある。**ON にしないこと**:

- **Require review from Code Owners**
  → bot は CODEOWNER ではないので承認しても満たせず、auto-merge が永久に止まる。

逆に問題ないもの:

- **Require approval from someone other than the last pusher**
  → PR を push したのは `dependabot[bot]`、承認するのは `github-actions[bot]` で別アカウント扱いなので満たせる。

## 確認方法

1. Dependabot の patch PR（例: ライブラリの `4.2.0 → 4.2.1`）が開くのを待つ（または `Settings → ... → Dependabot` から手動トリガ）。
2. PR ページで:
   - `github-actions[bot]` の **Approved** レビューが付く。
   - 「**Enable auto-merge**」済みの状態になり、CI 緑後に自動でマージされる。
3. 失敗時は PR の **Checks → dependabot-auto-merge** のログを見る。
   - approve で落ちる → 設定① を確認。
   - merge で `auto-merge is not allowed` → 設定② を確認。
   - 承認は付くがマージされない → 設定③ の required check 指定 or Code Owners 必須を確認。
