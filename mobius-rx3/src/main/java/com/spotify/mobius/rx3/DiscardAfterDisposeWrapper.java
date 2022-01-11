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
package com.spotify.mobius.rx3;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.Connection;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nullable;

/**
 * Wraps a {@link Connection} or a {@link Consumer} and blocks them from receiving any further
 * values after the wrapper has been disposed. Does not prevent races between {@link
 * #accept(Object)} and {@link #dispose()} for wrapped {@link Connection}s; the behaviour if such a
 * race happens is up to the original connection.
 */
class DiscardAfterDisposeWrapper<I> implements Consumer<I>, Disposable {
  private final Consumer<I> consumer;
  @Nullable private final Disposable disposable;
  private volatile boolean disposed;

  static <I> DiscardAfterDisposeWrapper<I> wrapConnection(Connection<I> connection) {
    checkNotNull(connection);
    return new DiscardAfterDisposeWrapper<>(connection, connection);
  }

  static <I> DiscardAfterDisposeWrapper<I> wrapConsumer(Consumer<I> consumer) {
    return new DiscardAfterDisposeWrapper<>(checkNotNull(consumer), null);
  }

  private DiscardAfterDisposeWrapper(Consumer<I> consumer, @Nullable Disposable disposable) {
    this.consumer = consumer;
    this.disposable = disposable;
  }

  @Override
  public void accept(I effect) {
    if (disposed) {
      return;
    }
    consumer.accept(effect);
  }

  @Override
  public void dispose() {
    disposed = true;
    if (disposable != null) {
      disposable.dispose();
    }
  }
}
