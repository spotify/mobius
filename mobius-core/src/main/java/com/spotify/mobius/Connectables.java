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
import javax.annotation.Nonnull;

/** TODO: document! */
public final class Connectables {
  private Connectables() {
    // prevent instantiation
  }

  public static <I, O> Connectable<I, O> fromRunnable(final Runnable action) {
    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            action.run();
          }

          @Override
          public void dispose() {}
        };
      }
    };
  }

  public static <I, O> Connectable<I, O> fromConsumer(final Consumer<I> consumer) {
    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            consumer.accept(value);
          }

          @Override
          public void dispose() {}
        };
      }
    };
  }
}
