import { redirect } from "next/navigation";

/** /admin は単体ページを持たず、 既定でブランド管理へ送る。 認証は admin/layout.tsx が担保する。 */
export default function AdminPage() {
  redirect("/admin/brands");
}
