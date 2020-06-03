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
package com.spotify.mobius.extras;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/**
 * A simple {@link Connectable} implementation that delegates creation of its {@link Connection} to
 * the specified factory.
 */
public final class SimpleConnectable<T, R> implements Connectable<T, R> {

  private final Function<Consumer<R>, Connection<T>> factory;

  public static <T, R> Connectable<T, R> withConnectionFactory(
      Function<Consumer<R>, Connection<T>> factory) {
    return new SimpleConnectable<>(checkNotNull(factory));
  }

  private SimpleConnectable(Function<Consumer<R>, Connection<T>> factory) {
    this.factory = factory;
  }

  @Nonnull
  @Override
  public Connection<T> connect(Consumer<R> output) throws ConnectionLimitExceededException {
    return factory.apply(output);
  }
}
