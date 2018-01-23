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
 * A {@link com.spotify.mobius.Connectable} that ensures that an inner {@link
 * com.spotify.mobius.Connection} doesn't emit or receive any values after being disposed.
 *
 * <p>This only acts as a safeguard, you still need to make sure that the Connectable disposes of
 * resources correctly.
 */
class SafeConnectable<F, E> implements Connectable<F, E> {

  private final Connectable<F, E> actual;

  SafeConnectable(Connectable<F, E> actual) {
    this.actual = checkNotNull(actual);
  }

  @Nonnull
  @Override
  public Connection<F> connect(Consumer<E> output) {
    final SafeConsumer<E> safeEventConsumer = new SafeConsumer<>(checkNotNull(output));
    final Connection<F> effectConsumer =
        new SafeEffectConsumer<>(checkNotNull(actual.connect(safeEventConsumer)));
    final Disposable disposable = CompositeDisposable.from(safeEventConsumer, effectConsumer);
    return new Connection<F>() {
      @Override
      public synchronized void accept(F effect) {
        effectConsumer.accept(effect);
      }

      @Override
      public synchronized void dispose() {
        disposable.dispose();
      }
    };
  }

  private static class SafeEffectConsumer<F> implements Connection<F> {
    private final Connection<F> actual;
    private boolean disposed;

    private SafeEffectConsumer(Connection<F> actual) {
      this.actual = actual;
    }

    @Override
    public synchronized void accept(F effect) {
      if (disposed) {
        return;
      }
      actual.accept(effect);
    }

    @Override
    public synchronized void dispose() {
      disposed = true;
      actual.dispose();
    }
  }

  private static class SafeConsumer<E> implements Connection<E> {
    private final Consumer<E> actual;
    private boolean disposed;

    private SafeConsumer(Consumer<E> actual) {
      this.actual = actual;
    }

    @Override
    public synchronized void accept(E value) {
      if (disposed) {
        return;
      }
      actual.accept(value);
    }

    @Override
    public synchronized void dispose() {
      disposed = true;
    }
  }
}
