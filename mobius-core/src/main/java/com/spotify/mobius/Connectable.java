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

import com.spotify.mobius.MobiusLoop.Controller;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/**
 * API for something that can be connected to be part of a {@link MobiusLoop}.
 *
 * <p>Primarily used in {@link Mobius#loop(Update, Connectable)} to define the effect handler of a
 * Mobius loop. In that case, the incoming values will be effects, and the outgoing values will be
 * events that should be sent back to the loop.
 *
 * <p>Alternatively used in {@link Controller#connect(Connectable)} to connect a view to the
 * controller. In that case, the incoming values will be models, and the outgoing values will be
 * events.
 *
 * @param <I> the incoming value type
 * @param <O> the outgoing value type
 */
public interface Connectable<I, O> {

  /**
   * Create a new connection that accepts input values and sends outgoing values to a supplied
   * consumer.
   *
   * <p>Must return a new {@link Connection} that accepts incoming values. After {@link
   * Connection#dispose()} } is called on the returned {@link Connection}, the connection must be
   * broken, and no more values may be sent to the output consumer.
   *
   * <p>Every call to this method should create a new independent connection that can be disposed of
   * individually without affecting the other connections. If your Connectable doesn't support this,
   * it should throw a {@link ConnectionLimitExceededException} if someone tries to connect a second
   * time before disposing of the first connection.
   *
   * @param output the consumer that the new connection should use to emit values
   * @return a new connection
   * @throws ConnectionLimitExceededException should be thrown if there are too many concurrent
   *     connections to this Connectable; this should be caused by incorrect usage of the
   *     Connectable, and is considered an irrecoverable error
   */
  @Nonnull
  Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException;
}
