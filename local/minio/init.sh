#!/bin/sh
# MinIO 初期化: bucket 作成 + 匿名 GET（<img> 表示用の公開読み取り）許可。
# MinIO は宣言的な「bucket 設定ファイル」を持たないので、mc クライアントで命令的に設定する。
set -e

# minio が起動するまでエイリアス登録をリトライ
until mc alias set local http://minio:9000 minioadmin minioadmin; do
  echo 'waiting minio'
  sleep 1
done

# 画像用 bucket を作成（既にあれば何もしない＝再起動しても安全）
mc mb --ignore-existing local/momiji-images
# 匿名ユーザーに GET(download) だけ許可 → <img src> で画像を直接表示できる（書き込みは presigned 経由のみ）
mc anonymous set download local/momiji-images
echo 'minio init done'
