package jp.momiji.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.allOk
import com.github.michaelbull.result.annotation.UnsafeResultValueAccess
import com.github.michaelbull.result.filterErr

// kotlin-resultのzipOrAccumulateは最大５個の引数にしか対応していないので、６個以上の引数に対応するための関数を定義する

@OptIn(UnsafeResultValueAccess::class)
inline fun <T1, T2, T3, T4, T5, T6, E, V> zipOrAccumulate(
    producer1: () -> Result<T1, E>,
    producer2: () -> Result<T2, E>,
    producer3: () -> Result<T3, E>,
    producer4: () -> Result<T4, E>,
    producer5: () -> Result<T5, E>,
    producer6: () -> Result<T6, E>,
    transform: (T1, T2, T3, T4, T5, T6) -> V,
): Result<V, List<E>> {
    val r1 = producer1()
    val r2 = producer2()
    val r3 = producer3()
    val r4 = producer4()
    val r5 = producer5()
    val r6 = producer6()

    val results: List<Result<*, E>> = listOf(r1, r2, r3, r4, r5, r6)
    return if (results.allOk()) {
        Ok(transform(r1.value, r2.value, r3.value, r4.value, r5.value, r6.value))
    } else {
        Err(results.filterErr())
    }
}

@OptIn(UnsafeResultValueAccess::class)
inline fun <T1, T2, T3, T4, T5, T6, T7, E, V> zipOrAccumulate(
    producer1: () -> Result<T1, E>,
    producer2: () -> Result<T2, E>,
    producer3: () -> Result<T3, E>,
    producer4: () -> Result<T4, E>,
    producer5: () -> Result<T5, E>,
    producer6: () -> Result<T6, E>,
    producer7: () -> Result<T7, E>,
    transform: (T1, T2, T3, T4, T5, T6, T7) -> V,
): Result<V, List<E>> {
    val r1 = producer1()
    val r2 = producer2()
    val r3 = producer3()
    val r4 = producer4()
    val r5 = producer5()
    val r6 = producer6()
    val r7 = producer7()

    val results: List<Result<*, E>> = listOf(r1, r2, r3, r4, r5, r6, r7)
    return if (results.allOk()) {
        Ok(transform(r1.value, r2.value, r3.value, r4.value, r5.value, r6.value, r7.value))
    } else {
        Err(results.filterErr())
    }
}
