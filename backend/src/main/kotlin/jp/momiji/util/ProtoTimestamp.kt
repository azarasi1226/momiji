package jp.momiji.util

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * read model の [LocalDateTime] を gRPC の [Timestamp]（絶対時刻）に変換する。
 *
 * read model の datetime は **UTC の壁掛け時計**として保存する規約（projector も `ZoneOffset.UTC` で保存）。
 * よってここでも **UTC として解釈**して `Instant` に直す → 保存と読み出しでゾーンが一致し往復で壊れない。
 * 表示用のタイムゾーン変換（日本時間など）は**フロントの責務**にして、 wire は常に UTC 絶対時刻で運ぶ。
 */
fun LocalDateTime.toProtoTimestamp(): Timestamp =
    timestamp {
        val instant = this@toProtoTimestamp.toInstant(ZoneOffset.UTC)
        seconds = instant.epochSecond
        nanos = instant.nano
    }

/**
 * gRPC の [Timestamp]（絶対時刻）を read model 比較用の [LocalDateTime] に戻す（[toProtoTimestamp] の逆）。
 *
 * read model の datetime は **UTC の壁掛け時計**で保存する規約なので、 ここでも **UTC として解釈**して
 * `LocalDateTime` に直す → 保存値と同じ土俵で比較できる（範囲フィルタの境界がズレない）。
 */
fun Timestamp.toUtcLocalDateTime(): LocalDateTime = LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC)
