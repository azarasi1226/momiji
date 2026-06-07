package jp.momiji.event

import org.axonframework.messaging.core.QualifiedName
import org.axonframework.messaging.eventhandling.annotation.Event
import kotlin.reflect.KClass

/**
 * `@Event(namespace, name)` を付けたイベントクラスから、 イベントストアに保存される
 * [QualifiedName] を復元する。
 *
 * DCB の `@EventCriteriaBuilder` で `andBeingOneOfTypes(QualifiedName...)` に渡す型名を作るために使う。
 * Axon は `@Event` 付きクラスを `MessageType(namespace, name, version)` として解決し、 その
 * `qualifiedName()` は `QualifiedName(namespace, name)` になる（= ここで作るものと一致）。
 *
 * **literal 文字列で型名を書かずこのヘルパで導出する**理由: イベント型名は `@Event` が単一の正
 * （ADR 0007）。 criteria 側で `QualifiedName("momiji.product", "ProductCreatedEvent")` と手書きすると、
 * `@Event` の値を変えたとき criteria が黙って一致しなくなる（再生で過去イベントを拾えずガードが
 * 効かなくなる）。 アノテーションから読めば常に追従する。
 */
fun KClass<*>.eventQualifiedName(): QualifiedName {
    val event =
        requireNotNull(java.getAnnotation(Event::class.java)) {
            "$qualifiedName に @Event が付いていません。 DCB criteria で参照するイベントには @Event が必須です"
        }
    return QualifiedName(event.namespace, event.name)
}
