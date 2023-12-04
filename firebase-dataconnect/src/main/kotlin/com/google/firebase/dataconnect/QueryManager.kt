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

import com.google.firebase.dataconnect.DataConnectGrpcClient.DeserialzedOperationResult
import com.google.firebase.dataconnect.DataConnectGrpcClient.OperationResult
import com.google.protobuf.Struct
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.DeserializationStrategy

internal class QueryManager(
  grpcClient: DataConnectGrpcClient,
  coroutineScope: CoroutineScope,
  deserializationDispatcher: CoroutineDispatcher,
  creatorLoggerId: String
) {
  private val logger = Logger("QueryManager").apply { debug { "Created from $creatorLoggerId" } }

  private val queryStates =
    QueryStates(grpcClient, coroutineScope, deserializationDispatcher, logger)

  suspend fun <V, D> execute(ref: QueryRef<V, D>, variables: V): DataConnectResult<V, D> =
    queryStates
      .withQueryState(ref, variables) { it.execute(ref.dataDeserializer) }
      .toDataConnectResult(variables)

  suspend fun <V, D> onResult(
    ref: QueryRef<V, D>,
    variables: V,
    sinceSequenceNumber: Long?,
    executeQuery: Boolean,
    callback: suspend (DataConnectResult<V, D>) -> Unit,
  ): Nothing =
    queryStates.withQueryState(ref, variables) { queryState ->
      queryState.onResult(
        ref.dataDeserializer,
        sinceSequenceNumber = sinceSequenceNumber,
        executeQuery = executeQuery
      ) {
        callback(it.toDataConnectResult(variables))
      }
    }
}

private data class QueryStateKey(val operationName: String, val variablesHash: String)

private class QueryState(
  val key: QueryStateKey,
  private val grpcClient: DataConnectGrpcClient,
  private val operationName: String,
  private val variables: Struct,
  private val coroutineScope: CoroutineScope,
  private val deserializationDispatcher: CoroutineDispatcher,
  private val logger: Logger,
) {
  // The `dataDeserializers` list may be safely read concurrently from multiple threads, as it uses
  // a `CopyOnWriteArrayList` that is completely thread-safe. Any mutating operations must be
  // performed while the `dataDeserializersWriteMutex` mutex is locked, so that
  // read-write-modify operations can be done atomically.
  private val dataDeserializersWriteMutex = Mutex()
  private val dataDeserializers = CopyOnWriteArrayList<RegisteredDataDeserialzer<*>>()

  private val jobMutex = Mutex()
  private var job: Job? = null

  suspend fun <T> execute(
    dataDeserializer: DeserializationStrategy<T>
  ): DeserialzedOperationResult<T> {
    // Register the data deserialzier _before_ waiting for the current job to complete. This
    // guarantees that the deserializer will be registered by the time the subsequent job (`newJob`
    // below) runs.
    val registeredDataDeserialzer =
      registerDataDeserializer(dataDeserializer, deserializationDispatcher)

    // Wait for the current job to complete (if any), and ignore its result. Waiting avoids running
    // multiple queries in parallel, which would not scale.
    val originalJob = jobMutex.withLock { job }?.also { it.join() }

    // Now that the job that was in progress when this method started has completed, we can run our
    // own query. But we're racing with other concurrent invocations of this method. The first one
    // wins and launches the new job, then awaits its completion; the others simply await completion
    // of the new job that was started by the winner.
    val newJob =
      jobMutex.withLock {
        job.let { currentJob ->
          if (currentJob !== null && currentJob !== originalJob) {
            currentJob
          } else {
            coroutineScope.async { doExecute() }.also { newJob -> job = newJob }
          }
        }
      }

    newJob.join()

    return registeredDataDeserialzer.getLatestUpdate()!!.getOrThrow()
  }

  suspend fun <T> onResult(
    dataDeserializer: DeserializationStrategy<T>,
    sinceSequenceNumber: Long?,
    executeQuery: Boolean,
    callback: suspend (DeserialzedOperationResult<T>) -> Unit,
  ): Nothing {
    val registeredDataDeserialzer =
      registerDataDeserializer(dataDeserializer, deserializationDispatcher)

    // Immediately deliver the most recent update to the callback, so the collector has some data
    // to work with while waiting for the network requests to complete.
    val cachedUpdate = registeredDataDeserialzer.getLatestSuccessfulUpdate()
    val effectiveSinceSequenceNumber =
      if (cachedUpdate === null) {
        sinceSequenceNumber
      } else if (
        sinceSequenceNumber !== null && sinceSequenceNumber >= cachedUpdate.sequenceNumber
      ) {
        sinceSequenceNumber
      } else {
        callback(cachedUpdate)
        cachedUpdate.sequenceNumber
      }

    // Execute the query _after_ delivering the cached result, so that collectors deterministically
    // get invoked with cached results first (if any), then updated results after the query
    // executes.
    if (executeQuery) {
      coroutineScope.launch { runCatching { execute(dataDeserializer) } }
    }

    registeredDataDeserialzer.onSuccessfulUpdate(
      sinceSequenceNumber = effectiveSinceSequenceNumber
    ) {
      callback(it)
    }
  }

  private suspend fun doExecute() {
    val requestId = Random.nextAlphanumericString()
    val sequenceNumber = nextSequenceNumber()

    val executeQueryResult =
      grpcClient.runCatching {
        executeQuery(
          requestId = requestId,
          operationName = operationName,
          variables = variables,
          sequenceNumber = sequenceNumber
        )
      }

    dataDeserializers.iterator().forEach {
      it.update(requestId, sequenceNumber, executeQueryResult)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private suspend fun <T> registerDataDeserializer(
    dataDeserializer: DeserializationStrategy<T>,
    deserializationDispatcher: CoroutineDispatcher,
  ): RegisteredDataDeserialzer<T> =
    // First, check if the deserializer is already registered and, if it is, just return it.
    // Otherwise, lock the "write" mutex and register it. We still have to check again if it is
    // already registered because another thread could have concurrently registered it since we last
    // checked above.
    dataDeserializers
      .firstOrNull { it.deserializer === dataDeserializer }
      ?.let { it as RegisteredDataDeserialzer<T> }
      ?: dataDeserializersWriteMutex.withLock {
        dataDeserializers
          .firstOrNull { it.deserializer === dataDeserializer }
          ?.let { it as RegisteredDataDeserialzer<T> }
          ?: RegisteredDataDeserialzer(dataDeserializer, deserializationDispatcher, logger).also {
            dataDeserializers.add(it)
          }
      }
}

private class RegisteredDataDeserialzer<T>(
  val deserializer: DeserializationStrategy<T>,
  private val deserializationDispatcher: CoroutineDispatcher,
  private val logger: Logger
) {
  private val latestUpdate = MutableStateFlow<Update<T>?>(null)
  private val latestSuccessfulUpdate = MutableStateFlow<DeserialzedOperationResult<T>?>(null)

  data class Update<T>(
    val sequenceNumber: Long,
    val result: Lazy<Result<DeserialzedOperationResult<T>>>
  )

  fun update(requestId: String, sequenceNumber: Long, result: Result<OperationResult>) {
    // Use a compare-and-swap ("CAS") loop to ensure that an old update never clobbers a newer one.
    val newUpdate =
      Update(sequenceNumber = sequenceNumber, result = lazyDeserialize(requestId, result))
    while (true) {
      val currentUpdate = latestUpdate.value
      if (currentUpdate !== null && currentUpdate.sequenceNumber > sequenceNumber) {
        return // don't clobber a newer update with an older one
      }
      if (latestUpdate.compareAndSet(currentUpdate, newUpdate)) {
        return
      }
    }
  }

  suspend fun getLatestUpdate(): Result<DeserialzedOperationResult<T>>? {
    val lazyResult = latestUpdate.value?.result ?: return null
    return if (lazyResult.isInitialized()) {
      lazyResult.value
    } else withContext(deserializationDispatcher) { lazyResult.value }
  }

  suspend fun getLatestSuccessfulUpdate(): DeserialzedOperationResult<T>? {
    // Call getLatestUpdate() to populate `latestSuccessfulUpdate` with the most recent update.
    getLatestUpdate()
    return latestSuccessfulUpdate.value
  }

  suspend fun onSuccessfulUpdate(
    sinceSequenceNumber: Long?,
    callback: suspend (DeserialzedOperationResult<T>) -> Unit
  ): Nothing {
    var lastSequenceNumber = sinceSequenceNumber ?: Long.MIN_VALUE
    latestUpdate.collect {
      if (it !== null && lastSequenceNumber < it.sequenceNumber) {
        val update =
          if (it.result.isInitialized()) {
            it.result.value
          } else withContext(deserializationDispatcher) { it.result.value }
        update.onSuccess {
          lastSequenceNumber = it.sequenceNumber
          callback(it)
        }
      }
    }
  }

  private fun lazyDeserialize(
    requestId: String,
    result: Result<OperationResult>
  ): Lazy<Result<DeserialzedOperationResult<T>>> = lazy {
    result
      .mapCatching {
        // TODO: move the deserialization to a different dispatcher because it could take a decent
        //  amount of time. As a general rule of thumb, suspend functions shouldn't perform blocking
        //  I/O or long-running CPU-bound operations on the calling thread since the calling thread
        //  could be the main thread.
        it.deserialize(deserializer)
      }
      .onFailure {
        // If the overall result was successful then the failure _must_ have occurred during
        // deserialization. Log the deserialization failure so it doesn't go unnoticed.
        if (result.isSuccess) {
          logger.warn(it) { "executeQuery() [rid=$requestId] decoding response data failed: $it" }
        }
      }
      .onSuccess {
        // Update the latest successful update. Set the value in a compare-and-swap loop to ensure
        // that an older result does not clobber a newer one.
        while (true) {
          val latestSuccessful = latestSuccessfulUpdate.value
          if (latestSuccessful !== null && latestSuccessful.sequenceNumber >= it.sequenceNumber) {
            break
          }
          if (latestSuccessfulUpdate.compareAndSet(latestSuccessful, it)) {
            break
          }
        }
      }
  }
}

private class QueryStates(
  private val grpcClient: DataConnectGrpcClient,
  private val coroutineScope: CoroutineScope,
  private val deserializationDispatcher: CoroutineDispatcher,
  private val logger: Logger,
) {
  private val mutex = Mutex()

  // NOTE: All accesses to `referenceCountedQueryStateByKey` and the `refCount` field of each value
  // MUST be done from a coroutine that has locked `mutex`; otherwise, such accesses (both reads and
  // writes) are data races and yield undefined behavior.
  private val referenceCountedQueryStateByKey =
    mutableMapOf<QueryStateKey, ReferenceCounted<QueryState>>()

  suspend fun <V, D, R> withQueryState(
    ref: QueryRef<V, D>,
    variables: V,
    block: suspend (QueryState) -> R
  ): R {
    val queryState = mutex.withLock { acquireQueryState(ref, variables) }

    return try {
      block(queryState)
    } finally {
      mutex.withLock { withContext(NonCancellable) { releaseQueryState(queryState) } }
    }
  }

  // NOTE: This function MUST be called from a coroutine that has locked `mutex`.
  private fun <V, D> acquireQueryState(ref: QueryRef<V, D>, variables: V): QueryState {
    val variablesStruct = encodeToStruct(ref.variablesSerializer, variables)
    val variablesHash = variablesStruct.calculateSha512().toAlphaNumericString()
    val key = QueryStateKey(operationName = ref.operationName, variablesHash = variablesHash)

    val referenceCountedQueryState =
      referenceCountedQueryStateByKey.getOrPut(key) {
        ReferenceCounted(
          QueryState(
            key = key,
            grpcClient = grpcClient,
            operationName = ref.operationName,
            variables = variablesStruct,
            coroutineScope = coroutineScope,
            deserializationDispatcher = deserializationDispatcher,
            logger = logger,
          ),
          refCount = 0
        )
      }

    referenceCountedQueryState.refCount++

    return referenceCountedQueryState.obj
  }

  // NOTE: This function MUST be called from a coroutine that has locked `mutex`.
  private fun releaseQueryState(queryState: QueryState) {
    val referenceCountedQueryState = referenceCountedQueryStateByKey[queryState.key]

    if (referenceCountedQueryState === null) {
      error("unexpected null QueryState for key: ${queryState.key}")
    } else if (referenceCountedQueryState.obj !== queryState) {
      error("unexpected QueryState for key: ${queryState.key}: ${referenceCountedQueryState.obj}")
    }

    referenceCountedQueryState.refCount--
    if (referenceCountedQueryState.refCount == 0) {
      referenceCountedQueryStateByKey.remove(queryState.key)
    }
  }
}