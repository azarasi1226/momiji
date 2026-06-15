import type { Metadata } from "next"
import Link from "next/link"
import { redirect } from "next/navigation"
import { CreditCard, MapPin } from "lucide-react"
import { auth } from "@/auth"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { fetchProfile } from "./actions"
import { ProfileForm } from "./profile-form"
import { EmailChangeForm } from "./email-change-form"
import { DeleteAccountButton } from "./delete-account-button"

export const metadata: Metadata = {
  title: "プロフィール",
}

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

        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col">
            <h2 className="text-lg font-medium">支払い方法</h2>
            <p className="text-sm text-muted-foreground">登録済みのクレジットカードを管理します。</p>
          </div>
          <Button asChild variant="outline" size="sm">
            <Link href="/profile/payment-methods">
              <CreditCard />
              管理する
            </Link>
          </Button>
        </div>

        <Separator />

        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col">
            <h2 className="text-lg font-medium">配送先</h2>
            <p className="text-sm text-muted-foreground">お届け先の住所を管理します。</p>
          </div>
          <Button asChild variant="outline" size="sm">
            <Link href="/profile/shipping-addresses">
              <MapPin />
              管理する
            </Link>
          </Button>
        </div>

        <Separator />

        <DeleteAccountButton />
      </main>
    </div>
  )
}
