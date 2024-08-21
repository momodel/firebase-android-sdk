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

package com.google.firebase.crashlytics.internal.common;

import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Utils */
@SuppressWarnings({"ResultOfMethodCallIgnored", "UnusedReturnValue"})
public final class Utils {
  /** Timeout in milliseconds for blocking on background threads. */
  private static final int BACKGROUND_TIMEOUT_MILLIS = 3_000;

  /** Timeout in milliseconds for blocking on the main thread. Be careful about ANRs. */
  private static final int MAIN_TIMEOUT_MILLIS = 2_000;

  /**
   * Blocks until the given Task completes, and then returns the value the Task was resolved with,
   * if successful. If the Task fails, an exception will be thrown, wrapping the Exception of the
   * Task. Blocking on Tasks is generally a bad idea, and you definitely should not block the main
   * thread waiting on one. But there are a couple of weird spots in our SDK where we really have no
   * choice. You should not use this method for any new code. And if you really do have to use it,
   * you should feel slightly bad about it.
   *
   * @param task the task to block on
   * @return the value that was returned by the task, if successful.
   * @throws InterruptedException if the method was interrupted
   * @throws TimeoutException if the method timed out while waiting for the task.
   * @deprecated Don't use this. Drain the worker instead.
   */
  @Deprecated
  public static <T> T awaitEvenIfOnMainThread(Task<T> task)
      throws InterruptedException, TimeoutException {
    CountDownLatch latch = new CountDownLatch(1);

    task.continueWith(
        TASK_CONTINUATION_EXECUTOR_SERVICE,
        unusedTask -> {
          latch.countDown();
          return null;
        });

    if (Looper.getMainLooper() == Looper.myLooper()) {
      latch.await(MAIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    } else {
      latch.await(BACKGROUND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    if (task.isSuccessful()) {
      return task.getResult();
    } else if (task.isCanceled()) {
      throw new CancellationException("Task is already canceled");
    } else if (task.isComplete()) {
      throw new IllegalStateException(task.getException());
    } else {
      throw new TimeoutException();
    }
  }

  /** Invokes latch.await(timeout, unit) uninterruptibly. */
  @CanIgnoreReturnValue
  public static boolean awaitUninterruptibly(CountDownLatch latch, long timeout, TimeUnit unit) {
    boolean interrupted = false;
    try {
      long remainingNanos = unit.toNanos(timeout);
      long end = System.nanoTime() + remainingNanos;

      while (true) {
        try {
          // CountDownLatch treats negative timeouts just like zero.
          return latch.await(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
          interrupted = true;
          remainingNanos = end - System.nanoTime();
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * ExecutorService that is used exclusively by the awaitEvenIfOnMainThread function. If the
   * Continuation which counts down the latch is called on the same thread which is waiting on the
   * latch, a deadlock will occur. A dedicated ExecutorService ensures that cannot happen.
   */
  private static final ExecutorService TASK_CONTINUATION_EXECUTOR_SERVICE =
      ExecutorUtils.buildSingleThreadExecutorService(
          "awaitEvenIfOnMainThread task continuation executor");

  private Utils() {}
}
