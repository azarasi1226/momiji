import Link from "next/link";
import { fetchCards } from "./actions";
import { AddCardForm } from "./add-card-form";
import { CardList } from "./card-list";

export default async function PaymentMethodsPage() {
  const cards = await fetchCards();

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-8 p-6">
      <div className="flex items-start justify-between">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold">支払い方法</h1>
          <p className="text-sm text-muted-foreground">
            登録済みのクレジットカードを管理します。
          </p>
        </div>
        <Link
          href="/profile"
          className="text-sm text-muted-foreground transition-colors hover:text-foreground"
        >
          プロフィールへ戻る
        </Link>
      </div>

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-medium">登録済みのカード</h2>
        <CardList cards={cards} />
      </section>

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-medium">カードを追加</h2>
        <AddCardForm />
      </section>
    </div>
  );
}
