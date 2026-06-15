import type { NextConfig } from "next";

// 必須環境変数が未設定の場合は起動時に loud に落とす（サイレントな誤設定を防ぐ）。
function requireEnv(key: string): string {
  const v = process.env[key];
  if (!v) throw new Error(`next.config: 環境変数 ${key} が未設定です`);
  return v;
}

function toProtocol(v: string): "http" | "https" {
  if (v === "http" || v === "https") return v;
  throw new Error(
    `next.config: IMAGE_PROTOCOL は "http" か "https" で設定してください (現在: "${v}")`,
  );
}

// TODO: 本番デプロイ時は以下の環境変数を CI/CD または実行環境に設定すること
//   IMAGE_PROTOCOL=https
//   IMAGE_HOSTNAME=<bucket>.s3.<region>.amazonaws.com
//   IMAGE_PORT は不要（省略すると標準ポートを使用）
const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: toProtocol(requireEnv("IMAGE_PROTOCOL")),
        hostname: requireEnv("IMAGE_HOSTNAME"),
        ...(process.env.IMAGE_PORT ? { port: process.env.IMAGE_PORT } : {}),
      },
    ],
  },
};

export default nextConfig;
