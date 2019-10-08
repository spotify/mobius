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
package com.spotify.mobius;

import com.spotify.mobius.functions.Consumer;

/**
 * A {@link Connection} that ensures that an inner {@link Consumer} doesn't get any values after
 * being disposed.
 */
class SafeConsumer<I> implements Connection<I> {
  private final Consumer<I> actual;
  private boolean disposed;

  SafeConsumer(Consumer<I> actual) {
    this.actual = actual;
  }

  @Override
  public synchronized void accept(I input) {
    if (disposed) {
      return;
    }
    actual.accept(input);
  }

  @Override
  public synchronized void dispose() {
    disposed = true;
  }
}
