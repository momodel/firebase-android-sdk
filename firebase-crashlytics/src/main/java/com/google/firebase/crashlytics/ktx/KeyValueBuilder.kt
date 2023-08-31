// Copyright 2020 Google LLC
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

package com.google.firebase.crashlytics.ktx

import com.google.firebase.crashlytics.FirebaseCrashlytics

/** Helper class to enable fluent syntax in [setCustomKeys] */
@Deprecated(
  "com.google.firebase.crashlyticsktx.KeyValueBuilder(private has been deprecated. Use `com.google.firebase.crashlyticsKeyValueBuilder(private` instead.",
  ReplaceWith(
    expression = "KeyValueBuilder(private",
    imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlyticsKeyValueBuilder"]
  )
)
class KeyValueBuilder(private val crashlytics: FirebaseCrashlytics) {

  /** Sets a custom key and value that are associated with reports. */
  @Deprecated(
    "com.google.firebase.crashlyticsktx. has been deprecated. Use `com.google.firebase.crashlytics` instead.",
    ReplaceWith(
      expression = "",
      imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlytics"]
    )
  )
  fun key(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)

  /** Sets a custom key and value that are associated with reports. */
  @Deprecated(
    "com.google.firebase.crashlyticsktx. has been deprecated. Use `com.google.firebase.crashlytics` instead.",
    ReplaceWith(
      expression = "",
      imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlytics"]
    )
  )
  fun key(key: String, value: Double) = crashlytics.setCustomKey(key, value)

  /** Sets a custom key and value that are associated with reports. */
  @Deprecated(
    "com.google.firebase.crashlyticsktx. has been deprecated. Use `com.google.firebase.crashlytics` instead.",
    ReplaceWith(
      expression = "",
      imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlytics"]
    )
  )
  fun key(key: String, value: Float) = crashlytics.setCustomKey(key, value)

  /** Sets a custom key and value that are associated with reports. */
  @Deprecated(
    "com.google.firebase.crashlyticsktx. has been deprecated. Use `com.google.firebase.crashlytics` instead.",
    ReplaceWith(
      expression = "",
      imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlytics"]
    )
  )
  fun key(key: String, value: Int) = crashlytics.setCustomKey(key, value)

  /** Sets a custom key and value that are associated with reports. */
  @Deprecated(
    "com.google.firebase.crashlyticsktx. has been deprecated. Use `com.google.firebase.crashlytics` instead.",
    ReplaceWith(
      expression = "",
      imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlytics"]
    )
  )
  fun key(key: String, value: Long) = crashlytics.setCustomKey(key, value)

  /** Sets a custom key and value that are associated with reports. */
  @Deprecated(
    "com.google.firebase.crashlyticsktx. has been deprecated. Use `com.google.firebase.crashlytics` instead.",
    ReplaceWith(
      expression = "",
      imports = ["com.google.firebase.Firebase", "com.google.firebase.crashlytics"]
    )
  )
  fun key(key: String, value: String) = crashlytics.setCustomKey(key, value)
}
