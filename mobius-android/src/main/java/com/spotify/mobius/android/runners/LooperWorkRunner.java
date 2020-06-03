/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
 * --
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
 * -/-/-
 */
package com.spotify.mobius.android.runners;

import android.os.Handler;
import android.os.Looper;
import com.spotify.mobius.runners.WorkRunner;

/** A work runner that uses a {@link Looper} to run work. */
public class LooperWorkRunner implements WorkRunner {
  private final Handler handler;
  private volatile boolean disposed;

  LooperWorkRunner(Looper looper) {
    this.handler = new Handler(looper);
  }

  /** Will cancel all Runnables posted to this looper. */
  @Override
  public void dispose() {
    handler.removeCallbacksAndMessages(null);
    disposed = true;
  }

  /**
   * Will post the provided runnable to the looper for processing.
   *
   * @param runnable the runnable you would like to execute
   */
  @Override
  public void post(Runnable runnable) {
    if (disposed) return;
    handler.post(runnable);
  }

  /**
   * Creates a {@link WorkRunner} backed by the provided {@link Looper}
   *
   * @param looper the looper to use for processing work
   * @return a {@link WorkRunner} that uses the provided {@link Looper} for processing work
   */
  public static LooperWorkRunner using(Looper looper) {
    return new LooperWorkRunner(looper);
  }
}
