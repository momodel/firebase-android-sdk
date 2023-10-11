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

package com.google.firebase.sessions

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import java.util.LinkedList
import java.util.UUID

/** Service for providing access to session data */
internal class SessionDataService : Service() {

  private val boundClients = mutableListOf<Messenger>()
  private var curSessionId: String? = null
  private var isAppInForeground: Boolean = false
  private var lastMsgTimeMs: Long = 0

  /** Handler of incoming messages from clients. */
  internal inner class IncomingHandler(
    context: Context,
    private val appContext: Context = context.applicationContext,
  ) : Handler(Looper.getMainLooper()) { // TODO(rothbutter) probably want to use our own executor

    override fun handleMessage(msg: Message) {
      if (lastMsgTimeMs > msg.getWhen()) {
        Log.i(TAG, "SERVICE: Received old message $msg. Ignoring")
        return
      }
      when (msg.what) {
        FOREGROUNDED -> handleForegrounding(msg)
        BACKGROUNDED -> handleBackgrounding(msg)
        else -> super.handleMessage(msg)
      }
      lastMsgTimeMs = msg.getWhen()
    }
  }

  fun handleForegrounding(msg: Message) {
    Log.i(TAG, "SERVICE: Activity foregrounding at ${msg.getWhen()}")

    if (curSessionId == null) {
      Log.i(TAG, "SERVICE: Cold start detected.")
      newSession()
    } else if (!isAppInForeground && msg.getWhen() - lastMsgTimeMs > MAX_BACKGROUND_MS) {
      Log.i(TAG, "SERVICE: Session too long in background. Creating new session.")
      newSession()
    }

    isAppInForeground = true
  }

  fun newSession() {
    curSessionId = UUID.randomUUID().toString()
    Log.i(TAG, "SERVICE: Broadcasting session $curSessionId to ${boundClients.size} clients")
    boundClients.forEach { sendSession(it) }
  }

  fun handleBackgrounding(msg: Message) {
    Log.i(TAG, "SERVICE: Activity backgrounding at ${msg.getWhen()}")
    isAppInForeground = false
  }

  fun sendSession(client: Messenger) {
    try {
      client.send(
        Message.obtain(null, SESSION_UPDATED, 0, 0).also {
          it.setData(Bundle().also { it.putString(SESSION_UPDATE_EXTRA, curSessionId) })
        }
      )
    } catch (e: Exception) {
      if (e is DeadObjectException) {
        Log.i(TAG, "SERVICE: Removing dead client from list: $client")
        boundClients.remove(client)
      } else {
        Log.e(TAG, "SERVICE: Unable to push new session to $client.", e)
      }
    }
  }

  override fun onBind(intent: Intent): IBinder? {
    Log.i(TAG, "SERVICE: Service bound")
    val messenger = Messenger(IncomingHandler(this))
    val callbackMessenger = getCallback(intent)
    if (callbackMessenger != null) {
      boundClients.add(callbackMessenger)
      sendSession(callbackMessenger)
      Log.i(TAG, "SERVICE: Stored callback to $callbackMessenger. Size: ${boundClients.size}")
    }
    return messenger.binder
  }

  private fun getCallback(intent: Intent): Messenger? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(CLIENT_CALLBACK_MESSENGER, Messenger::class.java)
    } else {
      intent.getParcelableExtra<Messenger>(CLIENT_CALLBACK_MESSENGER)
    }

  internal companion object {
    const val TAG = "SessionDataService"
    const val CLIENT_CALLBACK_MESSENGER = "ClientCallbackMessenger"
    const val SESSION_UPDATE_EXTRA = "SessionUpdateExtra"

    // TODO(bryanatkinson): Swap this out for the value coming from settings
    const val MAX_BACKGROUND_MS = 30000

    const val FOREGROUNDED = 1
    const val BACKGROUNDED = 2
    const val SESSION_UPDATED = 3

    private var testService: Messenger? = null
    private var testServiceBound: Boolean = false
    private val queuedMessages = LinkedList<Message>()

    internal class ClientUpdateHandler() : Handler(Looper.getMainLooper()) {
      override fun handleMessage(msg: Message) {
        when (msg.what) {
          SESSION_UPDATED ->
            handleSessionUpdate(msg.data?.getString(SESSION_UPDATE_EXTRA) ?: "no-id-given")
          else -> super.handleMessage(msg)
        }
      }

      fun handleSessionUpdate(sessionId: String) {
        Log.i(TAG, "CLIENT: Session update received: $sessionId")
      }
    }

    private val testServiceConnection =
      object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
          Log.i(TAG, "CLIENT: Connected to SessionDataService. Queue size ${queuedMessages.size}")
          testService = Messenger(service)
          testServiceBound = true
          val queueItr = queuedMessages.iterator()
          for (msg in queueItr) {
            Log.i(TAG, "CLIENT: sending queued message ${msg.what}")
            sendMessage(msg)
            queueItr.remove()
          }
        }

        override fun onServiceDisconnected(className: ComponentName) {
          Log.i(TAG, "CLIENT: Disconnected from SessionDataService")
          testService = null
          testServiceBound = false
        }
      }

    fun bind(appContext: Context): Unit {
      Intent(appContext, SessionDataService::class.java).also { intent ->
        Log.i(TAG, "CLIENT: Binding service to application.")
        // This is necessary for the onBind() to be called by each process
        intent.setAction(android.os.Process.myPid().toString())
        intent.putExtra(CLIENT_CALLBACK_MESSENGER, Messenger(ClientUpdateHandler()))
        appContext.bindService(
          intent,
          testServiceConnection,
          Context.BIND_IMPORTANT or Context.BIND_AUTO_CREATE
        )
      }
    }

    fun foregrounded(activity: Activity): Unit {
      sendMessage(FOREGROUNDED)
    }

    fun backgrounded(activity: Activity): Unit {
      sendMessage(BACKGROUNDED)
    }

    private fun sendMessage(messageCode: Int) {
      sendMessage(Message.obtain(null, messageCode, 0, 0))
    }

    private fun sendMessage(msg: Message) {
      if (testService != null) {
        try {
          testService?.send(msg)
        } catch (e: RemoteException) {
          Log.e(TAG, "CLIENT: Unable to deliver message: ${msg.what}")
        }
      } else {
        Log.i(TAG, "CLIENT: Queueing message ${msg.what}")
        queuedMessages.add(msg)
      }
    }
  }
}
