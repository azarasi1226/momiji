package jp.momiji

/**
 * 非同期で発生する状態の確定を待つためのリトライ・ヘルパ。
 *
 * Subscribing Event Processor は Axon Server から gRPC でイベントを受け取って
 * ハンドラを起動するため、 fixture.then() の戻りと EventHandler の副作用呼び出しの間に
 * 数十ミリ秒の遅延が出ることがある。 spy の verify(...) が即座に通らないケースでは
 * このヘルパでポーリングする。
 */
fun eventually(
    // 全テスト一気に走らせると PSEP が前テストの event を処理中で初動が遅れることがあるので余裕を取る
    timeoutMillis: Long = 5_000,
    intervalMillis: Long = 100,
    block: () -> Unit,
) {
    val start = System.currentTimeMillis()
    val deadline = start + timeoutMillis
    var last: Throwable? = null
    var attempt = 0
    while (System.currentTimeMillis() < deadline) {
        attempt++
        try {
            block()
            if (attempt > 1) {
                val elapsed = System.currentTimeMillis() - start
                println("[eventually] ${attempt}回目で成功（経過 ${elapsed}ms）")
            }
            return
        } catch (t: AssertionError) {
            // MockK の verify 失敗は AssertionError 系で投げられるのでこの catch で受かる
            last = t
        }
        println("[eventually] ${attempt}回目失敗、${intervalMillis}ms 後に再試行")
        Thread.sleep(intervalMillis)
    }
    val elapsed = System.currentTimeMillis() - start
    throw AssertionError(
        "eventually タイムアウト（${attempt}回試行 / 経過 ${elapsed}ms / timeout=${timeoutMillis}ms, interval=${intervalMillis}ms）",
        last,
    )
}
