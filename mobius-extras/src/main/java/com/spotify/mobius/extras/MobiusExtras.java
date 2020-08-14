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

import static com.spotify.mobius.Mobius.loop;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.Mobius;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.Next;
import com.spotify.mobius.Update;
import com.spotify.mobius.functions.BiFunction;
import javax.annotation.Nonnull;

/** Factory methods for the mobius-extra library. */
public final class MobiusExtras {

  private static final Connectable<?, ?> NOOP_CONNECTABLE =
      output ->
          new Connection<Object>() {
            @Override
            public void accept(Object value) {
              // Do nothing.
            }

            @Override
            public void dispose() {
              // Do nothing.
            }
          };

  private MobiusExtras() {
    // prevent instantiation
  }

  /**
   * Create a {@link MobiusLoop.Builder} to help you configure a MobiusLoop before starting it.
   *
   * <p>This is an alternative to {@link Mobius#loop(Update, Connectable)} if you want to set up a
   * simple loop without effects. The primary use of this is to explain how to setup a Mobius loop.
   *
   * <p>Once done configuring the loop you can start the loop using {@link
   * MobiusLoop.Factory#startFrom(Object)}.
   *
   * @param update the update function of the loop.
   * @return a {@link MobiusLoop.Builder} instance that you can further configure before starting
   *     the loop
   */
  @Nonnull
  public static <M, E> MobiusLoop.Builder<M, E, ?> beginnerLoop(final BiFunction<M, E, M> update) {
    //noinspection unchecked
    return loop(
        new Update<M, E, Object>() {
          @Nonnull
          @Override
          public Next<M, Object> update(M model, E event) {
            return Next.next(update.apply(model, event));
          }
        },
        (Connectable<Object, E>) NOOP_CONNECTABLE);
  }
}
