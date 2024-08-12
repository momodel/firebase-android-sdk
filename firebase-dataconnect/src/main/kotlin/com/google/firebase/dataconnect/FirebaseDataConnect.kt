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

import android.annotation.SuppressLint
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.app
import com.google.firebase.dataconnect.core.FirebaseDataConnectFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

/**
 * Firebase Data Connect is Firebase's first relational database solution for app developers to
 * build mobile and web applications using a fully managed PostgreSQL database powered by Cloud SQL.
 *
 * See
 * [https://firebase.google.com/products/data-connect](https://firebase.google.com/products/data-connect)
 * for full details about the Firebase Data Connect product.
 *
 * ### GraphQL Schema and Operation Definition
 *
 * The database schema and operations to query and mutate the data are authored in GraphQL and
 * uploaded to the server. Then, the queries and mutations can be executed by name, providing
 * variables along with the name to control their behavior. For example, a mutation that inserts a
 * row into a "people" table could be named "InsertPerson" and require a variable for the person's
 * name and a variable for the person's age. Similarly, a query to retrieve a row from the "person"
 * table by its ID could be named "GetPersonById" and require a variable for the person's ID.
 *
 * ### Usage with the Generated SDK
 *
 * [FirebaseDataConnect] is the entry point to the Firebase Data Connect API; however, it is mostly
 * intended to be an implementation detail for the code generated by Firebase's tools, which provide
 * a type-safe API for running the queries and mutations. The generated classes and functions are
 * colloquially referred to as the "generated SDK" and will encapsulate the API defined in this
 * package. Applications are generally recommended to use the "generated SDK" rather than using this
 * API directly to enjoy the benefits of a type-safe API.
 *
 * ### Obtaining Instances
 *
 * To obtain an instance of [FirebaseDataConnect] call [FirebaseDataConnect.Companion.getInstance].
 * If desired, when done with it, release the resources of the instance by calling
 * [FirebaseDataConnect.close]. To connect to the Data Connect Emulator (rather than the production
 * Data Connect service) call [FirebaseDataConnect.useEmulator]. To create [QueryRef] and
 * [MutationRef] instances for running queries and mutations, call [FirebaseDataConnect.query] and
 * [FirebaseDataConnect.mutation], respectively. To enable debug logging, which is especially useful
 * when reporting issues to Google, set [FirebaseDataConnect.Companion.logLevel] to [LogLevel.DEBUG]
 * .
 *
 * ### Integration with Kotlin Coroutines and Serialization
 *
 * The Firebase Data Connect API is designed as a Kotlin-only API, and integrates tightly with
 * [Kotlin Coroutines](https://developer.android.com/kotlin/coroutines) and
 * [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization). Applications should
 * ensure that they depend on these two official Kotlin extension libraries and enable the Kotlin
 * serialization Gradle plugin.
 *
 * All blocking operations are exposed as `suspend` functions, which can be safely called from the
 * main thread. Any blocking and/or CPU-intensive operations are moved off of the calling thread to
 * a background dispatcher.
 *
 * Data sent to the Data Connect server is serialized and data received from the Data Connect server
 * is deserialized using Kotlin's Serialization framework. Applications will typically enable the
 * official Kotlin Serialization Gradle plugin to automatically generate the serializers and
 * deserializers for classes annotated with `@Serializable`. Of course, applications are free to
 * write the serializers by hand as well.
 *
 * ### Release Notes
 *
 * Release notes for the Firebase Data Connect Android SDK will be published here until it is merged
 * into the `master` branch of https://github.com/firebase/firebase-android-sdk, at which point the
 * release notes will become part of the regular Android SDK releases.
 *
 * #### 16.0.0-alpha06 (not yet released)
 * - [#6177](https://github.com/firebase/firebase-android-sdk/pull/6177]) Added `equals` and
 * `hashCode` methods to [com.google.firebase.dataconnect.generated.GeneratedConnector]. This is
 * purely a cosmetic change, but requires using dataconnect emulator v1.1.18 (released May 23, 2024)
 * or later; otherwise, a compilation error like `Class 'FooConnector' is not abstract and does not
 * implement abstract member public abstract fun equals(other: Any?): Boolean defined in
 * com.google.firebase.dataconnect.generated.GeneratedConnector` will occur.
 *
 * #### 16.0.0-alpha05 (June 24, 2024)
 * - [#6003](https://github.com/firebase/firebase-android-sdk/pull/6003]) Fixed [close] to
 * _actually_ close the underlying grpc network resources. Also, added [suspendingClose] to allow
 * callers to wait for the asynchronous closing work to complete, such as in integration tests.
 * - [#6005](https://github.com/firebase/firebase-android-sdk/pull/6005) Fixed a StrictMode
 * violation upon the first network request being sent.
 * - [#6006](https://github.com/firebase/firebase-android-sdk/pull/6006) Improved debug logging of
 * GRPC requests and responses.
 * - [#6038](https://github.com/firebase/firebase-android-sdk/pull/6038) Fixed a bug with incorrect
 * Timestamp serialization due to miscalculation in timezone decoding.
 * - [#6052](https://github.com/firebase/firebase-android-sdk/pull/6052) Automatically retry
 * operations (queries and mutations) that fail due to an expired authentication token, with a new
 * authentication token.
 *
 * #### 16.0.0-alpha04 (May 29, 2024)
 * - [#5976](https://github.com/firebase/firebase-android-sdk/pull/5976) Fixed time zone issues when
 * serializing java.util.Date objects
 * - [#5996](https://github.com/firebase/firebase-android-sdk/pull/5996) Changed default port of
 * useEmulator() to 9399 (was 9510); this goes with a change to the Data Connect Emulator v1.1.19
 * (firebase-tools v13.10.2) that changes the default port to 9399.
 *
 * #### 16.0.0-alpha03 (May 15, 2024)
 * - KDoc comments added.
 * - OptionalVariable: fix potential NullPointerException in toString() and hashCode().
 * - TimestampSerializer: add support for time zones specified using +HH:MM or -HH:MM.
 *
 * #### 16.0.0-alpha02 (May 13, 2024)
 * - Internal code cleanup; no externally-visible changes.
 *
 * #### 16.0.0-alpha01 (May 08, 2024)
 * - Initial release.
 *
 * ### Safe for Concurrent Use
 *
 * All methods and properties of [FirebaseDataConnect] are thread-safe and may be safely called
 * and/or accessed concurrently from multiple threads and/or coroutines.
 *
 * ### Not Stable for Inheritance
 *
 * The [FirebaseDataConnect] interface is _not_ stable for inheritance in third-party libraries, as
 * new methods might be added to this interface or contracts of the existing methods can be changed.
 */
public interface FirebaseDataConnect : AutoCloseable {

  /**
   * The [FirebaseApp] instance with which this object is associated.
   *
   * The [FirebaseApp] object is used for things such as determining the project ID of the Firebase
   * project and the configuration of FirebaseAuth.
   *
   * @see [FirebaseDataConnect.Companion.getInstance]
   */
  public val app: FirebaseApp

  /**
   * The configuration of the Data Connect "connector" used to connect to the Data Connect service.
   *
   * @see [FirebaseDataConnect.Companion.getInstance]
   */
  public val config: ConnectorConfig

  /**
   * The settings of this [FirebaseDataConnect] object, that affect how it behaves.
   *
   * @see [FirebaseDataConnect.Companion.getInstance]
   */
  public val settings: DataConnectSettings

  /**
   * Configure this [FirebaseDataConnect] object to connect to the Data Connect Emulator.
   *
   * This method is typically called immediately after creation of the [FirebaseDataConnect] object
   * and must be called before any queries or mutations are executed. An exception will be thrown if
   * called after a query or mutation has been executed. Calling this method causes the values in
   * [DataConnectSettings.host] and [DataConnectSettings.sslEnabled] to be ignored.
   *
   * To start the Data Connect emulator from the command line, first install Firebase Tools as
   * documented at https://firebase.google.com/docs/emulator-suite/install_and_configure then run
   * `firebase emulators:start --only auth,dataconnect`. Enabling the "auth" emulator is only needed
   * if using [com.google.firebase.auth.FirebaseAuth] to authenticate users. You may also need to
   * specify `--project <projectId>` if the Firebase tools are unable to auto-detect the project ID.
   *
   * @param host The host name or IP address of the Data Connect emulator to which to connect. The
   * default value, 10.0.2.2, is a magic IP address that the Android Emulator aliases to the host
   * computer's equivalent of `localhost`.
   * @param port The TCP port of the Data Connect emulator to which to connect. The default value is
   * the default port used
   */
  public fun useEmulator(host: String = "10.0.2.2", port: Int = 9399)

  /**
   * Creates and returns a [QueryRef] for running the specified query.
   * @param operationName The value for [QueryRef.operationName] of the returned object.
   * @param variables The value for [QueryRef.variables] of the returned object.
   * @param dataDeserializer The value for [QueryRef.dataDeserializer] of the returned object.
   * @param variablesSerializer The value for [QueryRef.variablesSerializer] of the returned object.
   */
  public fun <Data, Variables> query(
    operationName: String,
    variables: Variables,
    dataDeserializer: DeserializationStrategy<Data>,
    variablesSerializer: SerializationStrategy<Variables>,
  ): QueryRef<Data, Variables>

  /**
   * Creates and returns a [MutationRef] for running the specified mutation.
   * @param operationName The value for [MutationRef.operationName] of the returned object.
   * @param variables The value for [MutationRef.variables] of the returned object.
   * @param dataDeserializer The value for [MutationRef.dataDeserializer] of the returned object.
   * @param variablesSerializer The value for [MutationRef.variablesSerializer] of the returned
   * object.
   */
  public fun <Data, Variables> mutation(
    operationName: String,
    variables: Variables,
    dataDeserializer: DeserializationStrategy<Data>,
    variablesSerializer: SerializationStrategy<Variables>,
  ): MutationRef<Data, Variables>

  /**
   * Releases the resources of this object and removes the instance from the instance cache
   * maintained by [FirebaseDataConnect.Companion.getInstance].
   *
   * This method returns immediately, possibly before in-flight queries and mutations are completed.
   * Any future attempts to execute queries or mutations returned from [query] or [mutation] will
   * immediately fail. To wait for the in-flight queries and mutations to complete, call
   * [suspendingClose] instead.
   *
   * It is safe to call this method multiple times. On subsequent invocations, if the previous
   * closing attempt failed then it will be re-tried.
   *
   * After this method returns, calling [FirebaseDataConnect.Companion.getInstance] with the same
   * [app] and [config] will return a new instance, rather than returning this instance.
   *
   * @see suspendingClose
   */
  override fun close()

  /**
   * A version of [close] that has the same semantics, but suspends until the asynchronous work is
   * complete.
   *
   * If the asynchronous work fails, then the exception from the asynchronous work is rethrown by
   * this method.
   *
   * Using this method in tests may be useful to ensure that this object is fully shut down after
   * each test case. This is especially true if tests create [FirebaseDataConnect] in rapid
   * succession which could starve resources if they are all active simultaneously. In those cases,
   * it may be a good idea to call [suspendingClose] instead of [close] to ensure that each instance
   * is fully shut down before a new one is created. In normal production applications, where
   * instances of [FirebaseDataConnect] are created infrequently, calling [close] should be
   * sufficient, and avoids having to create a [CoroutineScope] just to close the object.
   *
   * @see close
   */
  public suspend fun suspendingClose()

  /**
   * Compares this object with another object for equality, using the `===` operator.
   *
   * The implementation of this method simply uses referential equality. That is, two instances of
   * [FirebaseDataConnect] compare equal using this method if, and only if, they refer to the same
   * object, as determined by the `===` operator. Notably, this makes it suitable for instances of
   * [FirebaseDataConnect] to be used as keys in a [java.util.WeakHashMap] in order to store
   * supplementary information about the [FirebaseDataConnect] instance.
   *
   * @param other The object to compare to this for equality.
   * @return `other === this`
   */
  override fun equals(other: Any?): Boolean

  /**
   * Calculates and returns the hash code for this object.
   *
   * See [equals] for the special guarantees of referential equality that make instances of this
   * class suitable for usage as keys in a hash map.
   *
   * @return the hash code for this object.
   */
  override fun hashCode(): Int

  /**
   * Returns a string representation of this object, useful for debugging.
   *
   * The string representation is _not_ guaranteed to be stable and may change without notice at any
   * time. Therefore, the only recommended usage of the returned string is debugging and/or logging.
   * Namely, parsing the returned string or storing the returned string in non-volatile storage
   * should generally be avoided in order to be robust in case that the string representation
   * changes.
   *
   * @return a string representation of this object, which includes the class name and the values of
   * all public properties.
   */
  override fun toString(): String

  /**
   * The companion object for [FirebaseDataConnect], which provides extension methods and properties
   * that may be accessed qualified by the class, rather than an instance of the class.
   *
   * ### Safe for Concurrent Use
   *
   * All methods and properties of [Companion] are thread-safe and may be safely called and/or
   * accessed concurrently from multiple threads and/or coroutines.
   */
  public companion object
}

/**
 * Returns the instance of [FirebaseDataConnect] associated with the given [FirebaseApp] and
 * [ConnectorConfig], creating the [FirebaseDataConnect] instance if necessary.
 *
 * The instances of [FirebaseDataConnect] are keyed from the given [FirebaseApp], using the identity
 * comparison operator `===`, and the given [ConnectorConfig], using the equivalence operator `==`.
 * That is, the first invocation of this method with a given [FirebaseApp] and [ConnectorConfig]
 * will create and return a new [FirebaseDataConnect] instance that is associated with those
 * objects. A subsequent invocation with the same [FirebaseApp] object and an equal
 * [ConnectorConfig] will return the same [FirebaseDataConnect] instance that was returned from the
 * previous invocation.
 *
 * If a new [FirebaseDataConnect] instance is created, it will use the given [DataConnectSettings].
 * If an existing instance will be returned, then the given (or default) [DataConnectSettings] must
 * be equal to the [FirebaseDataConnect.settings] of the instance about to be returned; otherwise,
 * an exception is thrown.
 *
 * @param app The [FirebaseApp] instance with which the returned object is associated.
 * @param config The [ConnectorConfig] with which the returned object is associated.
 * @param settings The [DataConnectSettings] for the returned object to use.
 * @return The [FirebaseDataConnect] instance associated with the given [FirebaseApp] and
 * [ConnectorConfig], using the given [DataConnectSettings].
 */
@SuppressLint("FirebaseUseExplicitDependencies")
public fun FirebaseDataConnect.Companion.getInstance(
  app: FirebaseApp,
  config: ConnectorConfig,
  settings: DataConnectSettings = DataConnectSettings(),
): FirebaseDataConnect =
  app.get(FirebaseDataConnectFactory::class.java).get(config = config, settings = settings)

/**
 * Returns the instance of [FirebaseDataConnect] associated with the default [FirebaseApp] and the
 * given [ConnectorConfig], creating the [FirebaseDataConnect] instance if necessary.
 *
 * This method is a shorthand for calling `FirebaseDataConnect.getInstance(Firebase.app, config)` or
 * `FirebaseDataConnect.getInstance(Firebase.app, config, settings)`. See the documentation of that
 * method for full details.
 *
 * @param config The [ConnectorConfig] with which the returned object is associated.
 * @param settings The [DataConnectSettings] for the returned object to use.
 * @return The [FirebaseDataConnect] instance associated with the default [FirebaseApp] and the
 * given [ConnectorConfig], using the given [DataConnectSettings].
 */
public fun FirebaseDataConnect.Companion.getInstance(
  config: ConnectorConfig,
  settings: DataConnectSettings = DataConnectSettings()
): FirebaseDataConnect = getInstance(app = Firebase.app, config = config, settings = settings)

/**
 * The log level used by all [FirebaseDataConnect] instances.
 *
 * The default log level is [LogLevel.WARN]. Setting this to [LogLevel.DEBUG] will enable debug
 * logging, which is especially useful when reporting issues to Google or investigating problems
 * yourself. Setting it to [LogLevel.NONE] will disable all logging.
 */
public var FirebaseDataConnect.Companion.logLevel: LogLevel
  get() = com.google.firebase.dataconnect.core.logLevel
  set(newLogLevel) {
    com.google.firebase.dataconnect.core.logLevel = newLogLevel
  }
