package jp.momiji.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ProtoTimestampTest {
    @Test
    fun `LocalDateTime を UTC 絶対時刻の proto Timestamp に変換する`() {
        val ldt = LocalDateTime.of(2026, 6, 18, 12, 34, 56, 789)

        val ts = ldt.toProtoTimestamp()

        assertEquals(ldt.toEpochSecond(ZoneOffset.UTC), ts.seconds)
        assertEquals(789, ts.nanos)
    }

    @Test
    fun `toProtoTimestamp と toUtcLocalDateTime は往復で一致する（UTC の壁掛け時計）`() {
        val ldt = LocalDateTime.of(2026, 1, 2, 3, 4, 5, 123_000_000)

        assertEquals(ldt, ldt.toProtoTimestamp().toUtcLocalDateTime())
    }
}
