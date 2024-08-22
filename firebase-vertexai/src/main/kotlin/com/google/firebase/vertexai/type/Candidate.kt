/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.vertexai.type

/** Safety rating corresponding to a generated content. */
class SafetyRating
internal constructor(
  val category: HarmCategory,
  val probability: HarmProbability,
  val probabilityScore: Float = 0f,
  val blocked: Boolean? = null,
  val severity: HarmSeverity? = null,
  val severityScore: Float? = null
)

/**
 * Provides citation metadata for sourcing of content provided by the model between a given
 * [startIndex] and [endIndex].
 *
 * @property startIndex The beginning of the citation.
 * @property endIndex The end of the citation.
 * @property uri The URI of the cited work.
 * @property license The license under which the cited work is distributed.
 */
class CitationMetadata
internal constructor(
  val startIndex: Int = 0,
  val endIndex: Int,
  val uri: String? = null,
  val license: String? = null
)

/** The reason for content finishing. */
enum class FinishReason {
  /** A new and not yet supported value. */
  UNKNOWN,

  /** Reason is unspecified. */
  UNSPECIFIED,

  /** Model finished successfully and stopped. */
  STOP,

  /** Model hit the token limit. */
  MAX_TOKENS,

  /** [SafetySetting] prevented the model from outputting content. */
  SAFETY,

  /** Model began looping. */
  RECITATION,

  /** Model stopped for another reason. */
  OTHER
}
