import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      // ローカル MinIO（docker-compose で localhost:9000 で起動）
      { protocol: "http", hostname: "localhost", port: "9000" },
      // 本番デプロイ時は S3 バケットのホスト名を追加する
      // 例: { protocol: "https", hostname: "<bucket>.s3.<region>.amazonaws.com" }
    ],
  },
};

export default nextConfig;
