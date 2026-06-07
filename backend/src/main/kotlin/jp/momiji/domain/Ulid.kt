package jp.momiji.domain

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import de.huxhorn.sulky.ulid.ULID

/**
 * ULID 形式の ID 検証。
 *
 * ID は **専用の値オブジェクト型を作らず String のまま** モデル (command/event/State) に流す方針。
 * AF5 の @TargetEntityId / @EventTag は文字列タグを前提とし、 型付き id にすると
 * 「タグ文字列 ⇄ id 型」復元のため public な String コンストラクタが要る一方、 momiji の VO は
 * 検証バイパス防止で internal constructor のため両立しない。 よって ID は型で包まず、
 * ここで「検証して String を返す」だけに徹する。
 *
 * BrandId / UserId などエンティティを問わず、 gRPC 入口で [validate] を zipOrAccumulate に
 * 並べて使う。
 */
object Ulid {
    fun validate(input: String): Result<String, ValidationError> =
        try {
            ULID.parseULID(input)
            Ok(input)
        } catch (_: Exception) {
            Err(Invalid)
        }

    object Invalid : ValidationError("id", "IDはULID形式である必要があります")
}
