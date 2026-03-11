"use client"

import { useActionState, useState } from "react"
import {
  requestEmailChange,
  confirmEmailChange,
  type EmailChangeState,
} from "./actions"

export function EmailChangeForm({ currentEmail }: { currentEmail: string }) {
  const [step, setStep] = useState<"request" | "confirm">("request")

  const [requestState, requestAction, isRequesting] = useActionState<EmailChangeState, FormData>(
    async (prevState, formData) => {
      const result = await requestEmailChange(prevState, formData)
      if (result?.success) {
        setStep("confirm")
      }
      return result
    },
    null,
  )

  const [confirmState, confirmAction, isConfirming] = useActionState<EmailChangeState, FormData>(
    confirmEmailChange,
    null,
  )

  if (step === "confirm") {
    return (
      <div className="flex w-full flex-col gap-4">
        <h2 className="text-lg font-medium text-black dark:text-zinc-50">
          メールアドレス変更の確認
        </h2>
        <p className="text-sm text-zinc-500 dark:text-zinc-400">
          新しいメールアドレスに確認メールを送信しました。メールに記載されたトークンを入力してください。
        </p>

        <form action={confirmAction} className="flex w-full flex-col gap-4">
          <div className="flex flex-col gap-1">
            <label htmlFor="token" className="text-sm text-zinc-500 dark:text-zinc-400">
              確認トークン
            </label>
            <input
              id="token"
              name="token"
              type="text"
              required
              className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
            />
          </div>

          {confirmState?.error && (
            <p className="text-sm text-red-500">{confirmState.error}</p>
          )}
          {confirmState?.success && (
            <p className="text-sm text-green-600 dark:text-green-400">
              メールアドレスを変更しました
            </p>
          )}

          <div className="flex gap-4">
            <button
              type="submit"
              disabled={isConfirming}
              className="flex h-12 items-center justify-center rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
            >
              {isConfirming ? "確認中..." : "確認"}
            </button>
            <button
              type="button"
              onClick={() => {
                setStep("request")
              }}
              className="flex h-12 items-center justify-center rounded-full border border-solid border-black/[.08] px-8 transition-colors hover:border-transparent hover:bg-black/[.04] dark:border-white/[.145] dark:hover:bg-[#1a1a1a]"
            >
              戻る
            </button>
          </div>
        </form>
      </div>
    )
  }

  return (
    <div className="flex w-full flex-col gap-4">
      <h2 className="text-lg font-medium text-black dark:text-zinc-50">
        メールアドレスの変更
      </h2>
      <p className="text-sm text-zinc-500 dark:text-zinc-400">
        現在: {currentEmail}
      </p>

      <form action={requestAction} className="flex w-full flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label htmlFor="newEmail" className="text-sm text-zinc-500 dark:text-zinc-400">
            新しいメールアドレス
          </label>
          <input
            id="newEmail"
            name="newEmail"
            type="email"
            required
            className="rounded-lg border border-zinc-200 px-4 py-2 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-50"
          />
        </div>

        {requestState?.error && (
          <p className="text-sm text-red-500">{requestState.error}</p>
        )}

        <button
          type="submit"
          disabled={isRequesting}
          className="flex h-12 items-center justify-center rounded-full bg-foreground px-8 text-background transition-colors hover:bg-[#383838] disabled:opacity-50 dark:hover:bg-[#ccc]"
        >
          {isRequesting ? "送信中..." : "変更リクエスト送信"}
        </button>
      </form>
    </div>
  )
}
