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
package com.spotify.mobius.rx2;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;
import static com.spotify.mobius.rx2.DiscardAfterDisposeWrapper.wrapConnection;
import static com.spotify.mobius.rx2.DiscardAfterDisposeWrapper.wrapConsumer;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.disposables.CompositeDisposable;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/**
 * A {@link Connectable} that ensures that {@link Connection}s created by the wrapped {@link
 * Connectable} don't emit or receive any values after being disposed.
 *
 * <p>This only acts as a safeguard, you still need to make sure that the Connectable disposes of
 * resources correctly.
 */
class DiscardAfterDisposeConnectable<I, O> implements Connectable<I, O> {

  private final Connectable<I, O> actual;

  DiscardAfterDisposeConnectable(Connectable<I, O> actual) {
    this.actual = checkNotNull(actual);
  }

  @Nonnull
  @Override
  public Connection<I> connect(Consumer<O> output) {
    final DiscardAfterDisposeWrapper<O> safeOutput = wrapConsumer(output);
    final Connection<I> input = actual.connect(safeOutput);
    final DiscardAfterDisposeWrapper<I> safeInput = wrapConnection(input);

    final Disposable disposable = CompositeDisposable.from(safeInput, safeOutput);

    return new Connection<I>() {
      @Override
      public void accept(I effect) {
        safeInput.accept(effect);
      }

      @Override
      public void dispose() {
        disposable.dispose();
      }
    };
  }
}
