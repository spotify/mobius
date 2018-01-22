/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
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
package com.spotify.mobius.runners;

/**
 * A {@link WorkRunner} that immediately invokes the {@link Runnable} you post on the thread you
 * posted from.
 */
public class ImmediateWorkRunner implements WorkRunner {

  private boolean disposed;

  @Override
  public synchronized void post(Runnable runnable) {
    if (disposed) return;

    runnable.run();
  }

  @Override
  public synchronized void dispose() {
    disposed = true;
  }
}
