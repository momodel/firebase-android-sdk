// Copyright 2023 Google LLC
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
package com.google.firebase.dataconnect

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseAppLifecycleListener
import com.google.firebase.app
import com.google.protobuf.NullValue
import com.google.protobuf.Struct
import com.google.protobuf.Value
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write
import kotlinx.coroutines.CoroutineDispatcher

class FirebaseDataConnect
internal constructor(
  private val context: Context,
  private val appName: String,
  internal val projectId: String,
  internal val location: String,
  internal val service: String,
  private val creator: FirebaseDataConnectFactory
) {
  private val logger = LoggerImpl("FirebaseDataConnect", Logger.Level.DEBUG)

  private val lock = ReentrantReadWriteLock()
  private var settingsFrozen = false
  private var terminated = false

  var settings: FirebaseDataConnectSettings = FirebaseDataConnectSettings.defaultInstance
    get() {
      lock.read {
        return field
      }
    }
    set(value) {
      lock.write {
        if (terminated) {
          throw IllegalStateException("instance has been terminated")
        }
        if (settingsFrozen) {
          throw IllegalStateException("settings cannot be modified after they are used")
        }
        field = value
      }
    }

  private val grpcClint: DataConnectGrpcClient by lazy {
    lock.write {
      if (terminated) {
        throw IllegalStateException("instance has been terminated")
      }
      settingsFrozen = true

      DataConnectGrpcClient(
        context = context,
        projectId = projectId,
        location = location,
        service = service,
        hostName = settings.hostName,
        port = settings.port,
        sslEnabled = settings.sslEnabled,
      )
    }
  }

  fun executeQuery(operationName: String, variables: Map<String, Any?>): Struct =
    grpcClint.executeQuery(operationName, variables)

  fun executeMutation(operationName: String, variables: Map<String, Any?>): Struct =
    grpcClint.executeMutation(operationName, variables)

  fun terminate() {
    lock.write {
      grpcClint.close()
      terminated = true
      creator.remove(this)
    }
  }

  override fun toString(): String {
    return "FirebaseDataConnect" +
      "{appName=$appName, projectId=$projectId, location=$location, service=$service}"
  }

  companion object {
    fun getInstance(location: String, service: String): FirebaseDataConnect =
      getInstance(Firebase.app, location, service)

    fun getInstance(app: FirebaseApp, location: String, service: String): FirebaseDataConnect =
      app.get(FirebaseDataConnectFactory::class.java).run { get(location, service) }
  }
}

private fun structFromMap(map: Map<String, Any?>): Struct =
  Struct.newBuilder().run {
    map.keys.sorted().forEach { key -> putFields(key, protoValueFromObject(map[key])) }
    build()
  }

private fun protoValueFromObject(obj: Any?): Value =
  Value.newBuilder()
    .run {
      when (obj) {
        null -> setNullValue(NullValue.NULL_VALUE)
        is String -> setStringValue(obj)
        is Boolean -> setBoolValue(obj)
        is Double -> setNumberValue(obj)
        else -> throw IllegalArgumentException("unsupported value type: ${obj::class}")
      }
    }
    .build()

internal class FirebaseDataConnectFactory(
  private val context: Context,
  private val firebaseApp: FirebaseApp,
  private val backgroundDispatcher: CoroutineDispatcher,
  private val blockingDispatcher: CoroutineDispatcher,
) {

  private val firebaseAppLifecycleListener = FirebaseAppLifecycleListener { _, _ -> close() }
  init {
    firebaseApp.addLifecycleEventListener(firebaseAppLifecycleListener)
  }

  private data class InstanceCacheKey(
    val location: String,
    val service: String,
  )

  private val lock = ReentrantLock()
  private val instancesByCacheKey = mutableMapOf<InstanceCacheKey, FirebaseDataConnect>()
  private var closed = false

  fun get(location: String, service: String): FirebaseDataConnect {
    val key = InstanceCacheKey(location = location, service = service)
    lock.withLock {
      if (closed) {
        throw IllegalStateException("FirebaseApp has been deleted")
      }
      val cachedInstance = instancesByCacheKey[key]
      if (cachedInstance !== null) {
        return cachedInstance
      }

      val projectId = firebaseApp.options.projectId ?: "<unspecified project ID>"
      val newInstance =
        FirebaseDataConnect(
          context = context,
          appName = firebaseApp.name,
          projectId = projectId,
          location = location,
          service = service,
          creator = this
        )
      instancesByCacheKey[key] = newInstance
      return newInstance
    }
  }

  fun remove(instance: FirebaseDataConnect) {
    lock.withLock {
      val entries = instancesByCacheKey.entries.filter { it.value === instance }
      if (entries.isEmpty()) {
        return
      } else if (entries.size == 1) {
        instancesByCacheKey.remove(entries[0].key)
      } else {
        throw IllegalStateException(
          "internal error: FirebaseDataConnect instance $instance" +
            "maps to more than one key: ${entries.map { it.key }.joinToString(", ")}"
        )
      }
    }
  }

  private fun close() {
    val instances = mutableListOf<FirebaseDataConnect>()
    lock.withLock {
      closed = true
      instances.addAll(instancesByCacheKey.values)
    }

    instances.forEach { instance -> instance.terminate() }

    lock.withLock {
      if (instancesByCacheKey.isNotEmpty()) {
        throw IllegalStateException(
          "instances contains ${instances.size} instances " +
            "after calling terminate() on all FirebaseDataConnect instances, " +
            "but expected 0"
        )
      }
    }
  }
}
