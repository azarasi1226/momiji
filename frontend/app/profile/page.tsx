import Link from "next/link"
import { redirect } from "next/navigation"
import { auth } from "@/auth"
import { Separator } from "@/components/ui/separator"
import { fetchProfile } from "./actions"
import { ProfileForm } from "./profile-form"
import { EmailChangeForm } from "./email-change-form"
import { DeleteAccountButton } from "./delete-account-button"

export default async function ProfilePage() {
  const session = await auth()
  if (!session || session.error === "RefreshTokenError") {
    redirect("/")
  }

  const profile = await fetchProfile()

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 font-sans">
      <main className="flex w-full max-w-3xl flex-col items-center gap-8 bg-background px-16 py-16">
        <div className="flex w-full items-center justify-between">
          <h1 className="text-2xl font-semibold">プロフィール</h1>
          <Link
            href="/"
            className="text-sm text-muted-foreground transition-colors hover:text-foreground"
          >
            戻る
          </Link>
        </div>

        <ProfileForm profile={profile} />

        <Separator />

        <EmailChangeForm currentEmail={profile.email} />

        <Separator />

        <DeleteAccountButton />
      </main>
    </div>
  )
}
