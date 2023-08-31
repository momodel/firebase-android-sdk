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

package com.google.firebase.installations.ktx

import com.google.firebase.FirebaseApp
import com.google.firebase.components.Component
import com.google.firebase.components.ComponentRegistrar
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase

/** Returns the [FirebaseInstallations] instance of the default [FirebaseApp]. */
@Deprecated(
  "com.google.firebase.installationsktx.Firebase.installations has been deprecated. Use `com.google.firebase.installationsFirebase.installations` instead.",
  ReplaceWith(
    expression = "Firebase.installations",
    imports = ["com.google.firebase.Firebase", "com.google.firebase.installationsinstallations"]
  )
)
val Firebase.installations: FirebaseInstallations
  get() = FirebaseInstallations.getInstance()

/** Returns the [FirebaseInstallations] instance of a given [FirebaseApp]. */
@Deprecated(
  "com.google.firebase.installationsktx.Firebase.installations(app) has been deprecated. Use `com.google.firebase.installationsFirebase.installations(app)` instead.",
  ReplaceWith(
    expression = "Firebase.installations(app)",
    imports = ["com.google.firebase.Firebase", "com.google.firebase.installationsinstallations"]
  )
)
fun Firebase.installations(app: FirebaseApp): FirebaseInstallations =
  FirebaseInstallations.getInstance(app)

/** @suppress */
@Deprecated(
  "com.google.firebase.installationsktx.FirebaseInstallationsKtxRegistrar has been deprecated. Use `com.google.firebase.installationsFirebaseInstallationsKtxRegistrar` instead.",
  ReplaceWith(
    expression = "FirebaseInstallationsKtxRegistrar",
    imports =
      [
        "com.google.firebase.Firebase",
        "com.google.firebase.installationsFirebaseInstallationsKtxRegistrar"
      ]
  )
)
class FirebaseInstallationsKtxRegistrar : ComponentRegistrar {
  override fun getComponents(): List<Component<*>> = listOf()
}
