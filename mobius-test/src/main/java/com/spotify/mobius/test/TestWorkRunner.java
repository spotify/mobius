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
package com.spotify.mobius.test;

import com.spotify.mobius.runners.WorkRunner;
import java.util.ArrayDeque;
import java.util.Queue;

public class TestWorkRunner implements WorkRunner {

  private final Queue<Runnable> queue = new ArrayDeque<>();
  private boolean disposed;

  @Override
  public void post(Runnable runnable) {
    synchronized (queue) {
      if (disposed) {
        throw new IllegalStateException("this WorkRunner has already been disposed");
      }
      queue.add(runnable);
    }
  }

  private void runOne() {
    Runnable runnable;
    synchronized (queue) {
      if (queue.isEmpty()) return;
      runnable = queue.remove();
    }
    runnable.run();
  }

  public void runAll() {
    while (true) {
      synchronized (queue) {
        if (queue.isEmpty()) return;
      }
      runOne();
    }
  }

  @Override
  public void dispose() {
    synchronized (queue) {
      disposed = true;
      queue.clear();
    }
  }

  public boolean isDisposed() {
    return disposed;
  }
}
