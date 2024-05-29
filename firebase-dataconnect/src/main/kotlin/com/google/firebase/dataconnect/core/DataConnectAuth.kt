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

import com.google.firebase.auth.internal.IdTokenListener
import com.google.firebase.auth.internal.InternalAuthProvider
import com.google.firebase.dataconnect.util.nextSequenceNumber
import com.google.firebase.internal.InternalTokenResult
import com.google.firebase.internal.api.FirebaseNoSignedInUserException
import java.util.concurrent.Executor
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlinx.coroutines.tasks.*

internal class DataConnectAuth(
  deferredAuthProvider: com.google.firebase.inject.Deferred<InternalAuthProvider>,
  blockingExecutor: Executor,
  parentLogger: Logger,
) : AutoCloseable {
  private val logger =
    Logger("DataConnectAuth").apply { debug { "Created by ${parentLogger.nameWithId}" } }

  private val idTokenListener = IdTokenListener { runBlocking { onIdTokenChanged(it) } }

  private val mutex = Mutex()
  private var closed = false
  private var authProvider: InternalAuthProvider? = null
  private var tokenSequenceNumber = nextSequenceNumber()

  init {
    // Run `whenAvailable()` on a background thread because it accesses SharedPreferences, which
    // performs disk i/o, violating the StrictMode policy `android.os.strictmode.DiskReadViolation`.
    blockingExecutor.execute {
      deferredAuthProvider.whenAvailable {
        runBlocking { onInternalAuthProviderAvailable(it.get()) }
      }
    }
  }

  override fun close() = runBlocking {
    logger.debug { "close()" }
    mutex.withLock {
      closed = true
      authProvider?.removeIdTokenListener(idTokenListener)
      authProvider = null
    }
  }

  suspend fun getAccessToken(requestId: String): String? {
    while (true) {
      val (capturedAuthProvider, capturedTokenSequenceNumber) =
        mutex.withLock {
          val capturedAuthProvider = authProvider
          if (closed) {
            logger.debug {
              "[rid=$requestId] DataConnectAuth is closed; returning null access token"
            }
            return null
          } else if (capturedAuthProvider === null) {
            logger.debug {
              "[rid=$requestId] FirebaseAuth is not (yet?) available; " +
                "returning null access token"
            }
            return null
          }
          Pair(capturedAuthProvider, tokenSequenceNumber)
        }

      val accessTokenResult = capturedAuthProvider.runCatching { getAccessToken(false).await() }

      val tokenSequenceNumberChanged =
        mutex.withLock { capturedTokenSequenceNumber != tokenSequenceNumber }
      if (tokenSequenceNumberChanged) {
        logger.debug { "[rid=$requestId] token sequence number changed during fetch; re-fetching" }
        continue
      }

      if (accessTokenResult.isSuccess) {
        val accessToken = accessTokenResult.getOrNull()?.token
        if (accessToken === null) {
          logger.debug {
            "[rid=$requestId] returning null access token as provided by FirebaseAuth"
          }
          return null
        }
        logger.debug { "[rid=$requestId] got access token: ${accessToken.toScrubbedAccessToken()}" }
        return accessToken
      }

      val exception = accessTokenResult.exceptionOrNull()!!
      if (exception is FirebaseNoSignedInUserException) {
        logger.debug {
          "[rid=$requestId] FirebaseAuth has no signed-in user; returning null access token"
        }
        return null
      }

      logger.warn(exception) {
        "[rid=$requestId] getting access token failed for an unknown reason"
      }
      throw exception
    }
  }

  private suspend fun onInternalAuthProviderAvailable(newAuthProvider: InternalAuthProvider) =
    mutex.withLock {
      val newId = System.identityHashCode(newAuthProvider)

      if (closed) {
        return
      }
      if (authProvider === newAuthProvider) {
        return
      }
      if (authProvider !== null) {
        val oldId = System.identityHashCode(authProvider)
        logger.warn {
          "WARNING: Deferred<InternalAuthProvider>.whenAvailable() callback " +
            "was invoked multiple times, first with $oldId and then with $newId; " +
            "this is unexpected; ignoring $newId and continuing to use $oldId."
        }
        return
      }

      logger.debug { "onInternalAuthProviderAvailable($newId) setting InternalAuthProvider" }
      authProvider = newAuthProvider
      newAuthProvider.addIdTokenListener(idTokenListener)
    }

  private suspend fun onIdTokenChanged(result: InternalTokenResult) {
    logger.debug {
      "onIdTokenChanged(): token=${result.token?.uppercase()?.toScrubbedAccessToken()}"
    }
    mutex.withLock { tokenSequenceNumber = nextSequenceNumber() }
  }

  companion object {

    /**
     * Returns a new string that is equal to this string but only includes a chunk from the
     * beginning and the end.
     *
     * This method assumes that the contents of this string are an access token. The returned string
     * will have enough information to reason about the access token in logs without giving its
     * value away.
     */
    fun String.toScrubbedAccessToken(): String =
      if (length < 30) {
        "<redacted>"
      } else {
        buildString {
          append(this@toScrubbedAccessToken, 0, 6)
          append("<redacted>")
          append(
            this@toScrubbedAccessToken,
            this@toScrubbedAccessToken.length - 6,
            this@toScrubbedAccessToken.length
          )
        }
      }

    /**
     * Returns a new string that is equal to this string but with all occurrences of the given
     * access token with replaced with only the first and last 6 characters. This allows logging the
     * access token without completely giving it away.
     */
    fun String.withScrubbedAccessToken(accessToken: String): String =
      if (accessToken.isEmpty()) {
        this
      } else {
        replace(accessToken, accessToken.toScrubbedAccessToken())
      }
  }
}