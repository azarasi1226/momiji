import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"
import { fetchProfile } from "./actions"
import { ProfileForm } from "./profile-form"

export default async function ProfilePage() {
  const session = await auth()
  if (!session) {
    redirect("/")
  }

  const profile = await fetchProfile()

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-50 font-sans dark:bg-black">
      <main className="flex w-full max-w-3xl flex-col items-center gap-8 py-16 px-16 bg-white dark:bg-black">
        <div className="flex w-full items-center justify-between">
          <h1 className="text-2xl font-semibold text-black dark:text-zinc-50">
            プロフィール
          </h1>
          <Link
            href="/"
            className="text-sm text-zinc-500 hover:text-zinc-800 dark:text-zinc-400 dark:hover:text-zinc-200"
          >
            戻る
          </Link>
        </div>

        <ProfileForm profile={profile} />
      </main>
    </div>
  )
}
