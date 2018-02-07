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
package com.spotify.mobius.extras;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/** Contains utility functions for working with {@link Connectables}. */
public final class Connectables {

  private Connectables() {
    // prevent instantiation
  }

  @Nonnull
  public static <M, E, V> Connectable<M, E> map(
      final Function<M, V> mapper, final Connectable<V, E> connectable) {
    return new Connectable<M, E>() {
      @Nonnull
      @Override
      public Connection<M> connect(Consumer<E> output) throws ConnectionLimitExceededException {
        final Connection<V> delegateConnection = connectable.connect(output);

        return new Connection<M>() {
          @Override
          public void accept(M value) {
            delegateConnection.accept(mapper.apply(value));
          }

          @Override
          public void dispose() {
            delegateConnection.dispose();
          }
        };
      }
    };
  }
}
