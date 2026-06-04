package jp.momiji.config.otlp

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import io.opentelemetry.sdk.trace.samplers.SamplingResult
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val logger = KotlinLogging.logger {}

// Axon Event Processor が background スレッドで定期的に走らせる TokenEntry の
// SELECT FOR UPDATE / UPDATE や projection 系のクエリは、 gRPC リクエストとは独立に
// datasource-micrometer の JDBC observation によって新しい root span (parent なし) を作るため、
// 大量のゴミ trace が出る。
//
// 判定方針:
// Observation API 経由で作られた span は、 サンプラ呼び出し時点では name がまだ未設定
// (`<unspecified span name>`) で来るため name で判定できない。 そこで **SpanKind を見て判定** する。
//
//   - SpanKind.SERVER の root → 残す (gRPC server entry は SERVER、 これがエントリ正規ルート)
//   - SpanKind.CLIENT の root → drop (JDBC / 外向き gRPC、 上流リクエスト無しに走ってるのは背景処理確定)
//   - SpanKind.INTERNAL の root → drop (aspect / 自動 instrument が背景スレッドで打つ単発 span)
//   - SpanKind.PRODUCER / CONSUMER の root → 残す (messaging 起点、 今のスタックでは出ない)
//   - 親 span ありの span (gRPC リクエスト中の JDBC など) → parentBased が親の判断を継承
//
// 補足: spring-boot-micrometer-tracing-opentelemetry の `otelSampler` bean は
// `@ConditionalOnMissingBean` で gate されているので、 ここで `Sampler` 型の bean を出すと
// autoconfig のデフォルトサンプラ (probability based) を完全に置き換える形になる。
@Configuration
@Profile("observability-otlp")
class TracingSamplerConfig {
    @Bean
    fun otelSampler(): Sampler {
        return Sampler.parentBased(DropJdbcRootSampler())
    }
}

// `Sampler.parentBased(rootSampler)` の root sampler 部分。 つまり 「 親 span がいない時 ( = root span )
// にだけ呼ばれる 」 判定器。 親 span 有りの場合は parentBased が親の判断を継承するのでここまで来ない。
//
// file-private にしているのは外から使う必要が無いため (TracingSamplerConfig の中で完結)。
private class DropJdbcRootSampler : Sampler {
    // OpenTelemetry SDK は span を 1 個作るたびに shouldSample を呼ぶ。 ここで RECORD_AND_SAMPLE
    // を返すと「 記録 + export 」、 DROP を返すとその span と配下の子 span 全部が無かったことになる。
    override fun shouldSample(
        // 親 span のある Context (root sampler としては呼ばれる時点でほぼ空)。
        parentContext: Context,
        // 新しく作られる trace の ID (これから sample 対象の trace の識別子)。
        traceId: String,
        // 作られようとしている span の名前。 Observation API 経由だと未設定 (`<unspecified span name>`)
        // で渡ってくるため判定には使わない。
        name: String,
        // span の種別 (SERVER / CLIENT / INTERNAL など)。 これで JDBC / 外向きかどうかを判定する。
        spanKind: SpanKind,
        // span 作成時にすでに付いている attribute (datasource-micrometer は span 作成時に
        // `jdbc.datasource.name` 等を attach するのでここに入ってくる)。
        attributes: Attributes,
        // この span が link している他の trace への参照。 今回は使わない。
        parentLinks: List<LinkData>,
    ): SamplingResult {
        // root として現れたら drop する SpanKind:
        //   - CLIENT  : 上流コンテキスト無しでの外向き呼び出し ( Axon の JDBC ポーリング、 起動時の jOOQ クエリ等 )
        //   - INTERNAL: 上流コンテキスト無しの内部処理 ( aspect / 自動 instrument が背景スレッドで打つ単発 span 等 )
        // SERVER は gRPC エントリの正規 root なので残す。 PRODUCER / CONSUMER は今のスタックでは出ないが、
        // 出た場合は messaging の起点として残す。
        val drop = spanKind == SpanKind.CLIENT || spanKind == SpanKind.INTERNAL
        return if (drop) {
            SamplingResult.create(SamplingDecision.DROP)
        } else {
            SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE)
        }
    }

    // OpenTelemetry SDK の内部ログや diagnostic 用のサンプラ識別名。 機能には影響しないが、
    // 「 どのサンプラがどう判定したか 」 を追う時に役立つので明示的な文字列を返す。
    override fun getDescription(): String = "DropJdbcRootSampler"
}
