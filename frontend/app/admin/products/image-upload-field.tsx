"use client";

import Image from "next/image";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { issueImageUploadUrl } from "./actions";

const ALLOWED_TYPES = ["image/png", "image/jpeg", "image/webp"];
const MAX_BYTES = 5 * 1024 * 1024; // 5MB

/**
 * 画像アップロード欄。 ファイルを選ぶと:
 *  1) backend に presigned PUT URL を発行させ
 *  2) ブラウザが S3/MinIO へ直接 PUT し
 *  3) 返ってきた恒久 public URL を hidden input([name]) に入れる
 * → そのまま親フォームの create/update に乗る（imageUrl 文字列として送信）。
 */
export function ImageUploadField({
  name = "imageUrl",
  defaultUrl = "",
}: {
  name?: string;
  defaultUrl?: string;
}) {
  const [url, setUrl] = useState(defaultUrl);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);

    if (!ALLOWED_TYPES.includes(file.type)) {
      setError("png / jpeg / webp のみアップロードできます");
      return;
    }
    if (file.size > MAX_BYTES) {
      setError("ファイルサイズは 5MB 以下にしてください");
      return;
    }

    setUploading(true);
    try {
      const result = await issueImageUploadUrl(file.type);
      if ("error" in result) {
        setError(result.error);
        return;
      }
      const res = await fetch(result.uploadUrl, {
        method: "PUT",
        headers: { "Content-Type": file.type },
        body: file,
      });
      if (!res.ok) throw new Error(`upload failed: ${res.status}`);
      setUrl(result.publicUrl);
    } catch {
      setError("アップロードに失敗しました");
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      {/* 親フォームへは URL 文字列として送る */}
      <input type="hidden" name={name} value={url} />

      {url && (
        <Image
          src={url}
          alt="プレビュー"
          width={160}
          height={160}
          className="rounded-lg border object-cover"
        />
      )}

      <div className="flex items-center gap-2">
        <Input
          type="file"
          accept="image/png,image/jpeg,image/webp"
          onChange={handleChange}
          disabled={uploading}
          className="w-auto"
        />
        {url && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setUrl("")}
          >
            画像を外す
          </Button>
        )}
      </div>

      {uploading && (
        <p className="text-xs text-muted-foreground">アップロード中...</p>
      )}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}
