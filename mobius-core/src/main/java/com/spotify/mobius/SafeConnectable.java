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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.disposables.CompositeDisposable;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/**
 * A {@link Connectable} that ensures that an inner {@link Connection} doesn't emit or receive any
 * values after being disposed.
 *
 * <p>This only acts as a safeguard, you still need to make sure that the Connectable disposes of
 * resources correctly.
 */
class SafeConnectable<I, O> implements Connectable<I, O> {

  private final Connectable<I, O> actual;

  SafeConnectable(Connectable<I, O> actual) {
    this.actual = checkNotNull(actual);
  }

  @Nonnull
  @Override
  public Connection<I> connect(Consumer<O> output) {
    final SafeConsumer<O> safeOutput = new SafeConsumer<>(checkNotNull(output));

    final Connection<I> connection = checkNotNull(actual.connect(safeOutput));
    final Connection<I> safeConnection = new SafeConnection<>(connection);

    final Disposable disposable = CompositeDisposable.from(safeOutput, safeConnection);

    return new Connection<I>() {
      @Override
      public synchronized void accept(I input) {
        safeConnection.accept(input);
      }

      @Override
      public synchronized void dispose() {
        disposable.dispose();
      }
    };
  }
}
