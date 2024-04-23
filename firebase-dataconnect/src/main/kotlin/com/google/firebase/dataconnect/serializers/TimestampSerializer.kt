// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.firebase.dataconnect.serializers

import com.google.firebase.Timestamp
import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object TimestampSerializer : KSerializer<Timestamp> {
  private val threadLocalDateFormatter =
    object : ThreadLocal<SimpleDateFormat>() {
      override fun initialValue(): SimpleDateFormat {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat
      }
    }

  private val dateFormatter: DateFormat
    get() = threadLocalDateFormatter.get()!!

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

  /**
   * The expected serialized timestamp format is RFC3339: `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'`, it
   * can be constructed by two parts. First, we use `dateFormatter` to serialize seconds. Then, we
   * pad nanoseconds into a 9 digits string.
   */
  private fun timestampToString(timestamp: Timestamp): String {
    val serializedSecond = dateFormatter.format(Date(timestamp.seconds * 1000))
    val serializedNano = timestamp.nanoseconds.toString().padStart(9, '0').takeLast(9)
    return "$serializedSecond.${serializedNano}Z"
  }

  /**
   * Note: Timestamp sent from SDK is always 9 digits nanosecond precision, meaning there are 9
   * digits in SSSSSSSSS parts. However, when running against different databases, this precision
   * might change, and server will truncate it to 0/3/6 digits precision without throwing an error.
   */
  private fun timestampFromString(str: String): Timestamp {
    // Split `yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'` into `yyyy-MM-dd'T'HH:mm:ss` and SSSSSSSSS'Z'
    val parts = str.split(".")

    // Seconds
    val position = ParsePosition(0)
    val date = dateFormatter.parse(str, position)
    requireNotNull(date)
    val seconds = Timestamp(date).seconds

    // Nanoseconds
    val nanosecondsStr = parts[1].replace("Z", "")
    val nanoseconds = if (nanosecondsStr.length > 1) nanosecondsStr.toInt() else 0
    return Timestamp(seconds, nanoseconds)
  }

  override fun serialize(encoder: Encoder, value: Timestamp) {
    val rfc3339String = timestampToString(value)
    encoder.encodeString(rfc3339String)
  }

  override fun deserialize(decoder: Decoder): Timestamp {
    val rfc3339String = decoder.decodeString()
    return timestampFromString(rfc3339String)
  }
}