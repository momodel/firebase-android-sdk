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

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.google.firebase.dataconnect.demo"
  compileSdk = 33

  defaultConfig {
    minSdk = 33
    targetSdk = 33
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(project(":firebase-dataconnect"))

  implementation("androidx.core:core-ktx:1.10.1")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.9.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
  implementation("androidx.navigation:navigation-ui-ktx:2.6.0")

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)

  androidTestImplementation(libs.androidx.test.junit)
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
