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

package com.google.firebase.dataconnect.core

import android.os.Build
import com.google.firebase.dataconnect.BuildConfig
import com.google.firebase.dataconnect.util.buildStructProto
import com.google.protobuf.Struct
import io.grpc.Metadata

internal class DataConnectGrpcMetadata(
  val dataConnectAuth: DataConnectAuth,
  val dataConnectAppCheck: DataConnectAppCheck,
  val connectorLocation: String,
  val kotlinVersion: String,
  val androidVersion: Int,
  val dataConnectSdkVersion: String,
  val grpcVersion: String,
  val parentLogger: Logger,
) {
  private val logger =
    Logger("DataConnectGrpcMetadata").apply {
      debug {
        "created by ${parentLogger.nameWithId} with" +
          " dataConnectAuth=${dataConnectAuth.instanceId}" +
          " connectorLocation=$connectorLocation" +
          " kotlinVersion=$kotlinVersion" +
          " androidVersion=$androidVersion" +
          " dataConnectSdkVersion=$dataConnectSdkVersion" +
          " grpcVersion=$grpcVersion"
      }
    }
  val instanceId: String
    get() = logger.nameWithId

  @Suppress("SpellCheckingInspection")
  private val googRequestParamsHeaderValue = "location=${connectorLocation}&frontend=data"

  private fun googApiClientHeaderValue(isFromGeneratedSdk: Boolean): String {
    val components = buildList {
      add("gl-kotlin/$kotlinVersion")
      add("gl-android/$androidVersion")
      add("fire/$dataConnectSdkVersion")
      add("grpc/$grpcVersion")
      if (isFromGeneratedSdk) {
        add("kotlin/gen")
      }
    }
    return components.joinToString(" ")
  }

  suspend fun get(requestId: String, isFromGeneratedSdk: Boolean): Metadata {
    val authToken = dataConnectAuth.getToken(requestId)
    val appCheckToken = dataConnectAppCheck.getToken(requestId)
    return Metadata().also {
      it.put(googRequestParamsHeader, googRequestParamsHeaderValue)
      it.put(googApiClientHeader, googApiClientHeaderValue(isFromGeneratedSdk))
      if (authToken !== null) {
        it.put(firebaseAuthTokenHeader, authToken)
      }
      if (appCheckToken !== null) {
        it.put(firebaseAppCheckTokenHeader, appCheckToken)
      }
    }
  }

  companion object {
    fun Metadata.toStructProto(): Struct = buildStructProto {
      val keys: List<Metadata.Key<String>> = run {
        val keySet: MutableSet<String> = keys().toMutableSet()
        // Always explicitly include the auth header in the returned string, even if it is absent.
        keySet.add(firebaseAuthTokenHeader.name())
        keySet.add(firebaseAppCheckTokenHeader.name())
        keySet.sorted().map { Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER) }
      }

      for (key in keys) {
        val values = getAll(key)
        val scrubbedValues =
          if (values === null) listOf(null)
          else {
            values.map {
              when (key.name()) {
                firebaseAuthTokenHeader.name() -> it.toScrubbedAccessToken()
                firebaseAppCheckTokenHeader.name() -> it.toScrubbedAccessToken()
                else -> it
              }
            }
          }

        for (scrubbedValue in scrubbedValues) {
          put(key.name(), scrubbedValue)
        }
      }
    }

    private val firebaseAuthTokenHeader: Metadata.Key<String> =
      Metadata.Key.of("x-firebase-auth-token", Metadata.ASCII_STRING_MARSHALLER)

    private val firebaseAppCheckTokenHeader: Metadata.Key<String> =
      Metadata.Key.of("x-firebase-appcheck", Metadata.ASCII_STRING_MARSHALLER)

    @Suppress("SpellCheckingInspection")
    private val googRequestParamsHeader: Metadata.Key<String> =
      Metadata.Key.of("x-goog-request-params", Metadata.ASCII_STRING_MARSHALLER)

    @Suppress("SpellCheckingInspection")
    private val googApiClientHeader: Metadata.Key<String> =
      Metadata.Key.of("x-goog-api-client", Metadata.ASCII_STRING_MARSHALLER)

    fun forSystemVersions(
      dataConnectAuth: DataConnectAuth,
      dataConnectAppCheck: DataConnectAppCheck,
      connectorLocation: String,
      parentLogger: Logger,
    ): DataConnectGrpcMetadata =
      DataConnectGrpcMetadata(
        dataConnectAuth = dataConnectAuth,
        dataConnectAppCheck = dataConnectAppCheck,
        connectorLocation = connectorLocation,
        kotlinVersion = "${KotlinVersion.CURRENT}",
        androidVersion = Build.VERSION.SDK_INT,
        dataConnectSdkVersion = BuildConfig.VERSION_NAME,
        grpcVersion = "", // no way to get the grpc version at runtime,
        parentLogger = parentLogger,
      )
  }
}
