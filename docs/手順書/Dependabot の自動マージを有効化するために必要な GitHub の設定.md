# Dependabot の自動マージを有効化するために必要な GitHub の設定

Dependabot の **patch アップデートだけ**を自動マージしつつ、それ以外の PR は**レビュー必須**にするための GitHub リポジトリ設定をまとめる。

ワークフロー実体は [`.github/workflows/dependabot-auto-merge.yaml`](../.github/workflows/dependabot-auto-merge.yaml)。

## 実現したい運用

| 種類 | 挙動 |
| --- | --- |
| Dependabot の **patch** アップデート（`x.y."Z"`） | ワークフローが `github-actions[bot]` で承認 → required check 通過後に**自動マージ** |
| Dependabot の **minor / major** アップデート | ワークフローは承認しない → **人間による検証・レビュー必須**（通常 PR と同じ） |
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

**Github画面**: `Settings → Actions → General`　に遷移し
**☑Allow GitHub Actions to create and approve pull requests** という項目のチェックを付ける

### ② auto-merge を許可する

**Github画面**:`Settings → General → Pull Requests` に遷移し
**☑Allow auto-merge** という項目のチェックを付ける

### ③ レビュー必須の branch protection を追加する

`Settings → Branches`（または `Settings → Rules → Rulesets`）で `main` に対してルールを追加:

- ☑ **Require a pull request before merging**
  - ☑ **Require approvals** → 承認数 **1**
- ☑ **Require status checks to pass before merging**
  - 必須チェックに **Pull Request Verification** の各ジョブ（`proto-lint` / `backend` / `frontend`）を指定

> status check を必須にしておくことで、すべての CI 緑になるまで待たないとマージされなくなる。  
> required check を 1 つも指定しないと、auto-merge は承認直後に即マージしてしまい CI を待たない。
