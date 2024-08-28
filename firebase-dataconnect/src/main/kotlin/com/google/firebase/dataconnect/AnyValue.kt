/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.dataconnect

import com.google.firebase.dataconnect.util.encodeToStruct
import com.google.firebase.dataconnect.util.nullProtoValue
import com.google.firebase.dataconnect.util.toAny
import com.google.firebase.dataconnect.util.toCompactString
import com.google.firebase.dataconnect.util.toValueProto
import com.google.protobuf.Struct
import com.google.protobuf.Value
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

public class AnyValue internal constructor(internal val protoValue: Value) {

  init {
    require(protoValue.kindCase != Value.KindCase.NULL_VALUE) {
      "NULL_VALUE is not allowed; just use null"
    }
  }

  internal constructor(struct: Struct) : this(struct.toValueProto())

  public constructor(value: Map<String, Any?>) : this(value.toValueProto())

  public constructor(value: List<Any?>) : this(value.toValueProto())

  public constructor(value: String) : this(value.toValueProto())

  public constructor(value: Boolean) : this(value.toValueProto())

  public constructor(value: Double) : this(value.toValueProto())

  public constructor(@Suppress("UNUSED_PARAMETER") value: Nothing?) : this(nullProtoValue)

  public val value: Any
    // NOTE: The not-null assertion operator (!!) below will never throw because the `init` block
    // of this class asserts that `protoValue` is not NULL_VALUE.
    get() = protoValue.toAny()!!

  override fun hashCode(): Int = value.hashCode()

  override fun equals(other: Any?): Boolean = other is AnyValue && other.value == value

  override fun toString(): String = protoValue.toCompactString(keySortSelector = { it })

  public companion object {

    public fun <T> from(value: T, serializer: SerializationStrategy<T>): AnyValue =
      AnyValue(encodeToStruct(serializer, value))

    public inline fun <reified T> fromSerializable(value: T): AnyValue = from(value, serializer())

    @JvmName("fromAnyOrNull")
    public fun fromAny(value: Any?): AnyValue? = if (value === null) null else fromAny(value)

    public fun fromAny(value: Any): AnyValue {
      @Suppress("UNCHECKED_CAST")
      return when (value) {
        is String -> AnyValue(value)
        is Boolean -> AnyValue(value)
        is Double -> AnyValue(value)
        is List<*> -> AnyValue(value)
        is Map<*, *> -> AnyValue(value as Map<String, Any?>)
        else ->
          throw IllegalArgumentException(
            "unsupported type: ${value::class.qualifiedName}" +
              " (supported types: null, String, Boolean, Double, List<Any?>, Map<String, Any?>)"
          )
      }
    }
  }
}
