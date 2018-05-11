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

import com.spotify.mobius.functions.BiConsumer;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;

/**
 * Builder for a effect handler that routes to different sub-handlers based on effect type.
 *
 * <p>Register handlers for different subtypes of F using the addXXX() methods, and call {@link
 * #build()} to create an instance of the effect handler. You can then create a loop with the router
 * as the effect handler using {@link Mobius#loop(Update, Connectable)}.
 *
 * <p>The router will look at the type of each incoming effect object and try to find a registered
 * handler for that particular subtype of F. If a handler is found, it will be given the effect
 * object, otherwise an {@link UnknownEffectException} will be thrown.
 *
 * <p>All the classes that the effect router know about must have a common type F. Note that
 * instances of the builder are mutable and not thread-safe.
 */
public interface EffectRouterBuilder<F, E> {

  /**
   * Add a {@link Runnable} for handling effects of a given type. The runnable will be invoked for
   * each incoming effect object that extends the {@code effectClass}. This is useful for cases when
   * the effect doesn't carry any data other than that it happened and no effect feedback events
   * will be generated.
   *
   * <p>Adding handlers for two effect classes where one is a super-class of the other is considered
   * a collision and is not allowed. Registering the same class twice is also considered a
   * collision.
   *
   * @param effectClass the effect class to handle
   * @param action the effect handler for the given effect class
   * @param <G> the effect class as a type parameter
   * @return this builder
   * @throws IllegalArgumentException if there is a handler collision
   */
  <G extends F> EffectRouterBuilder<F, E> addRunnable(Class<G> effectClass, Runnable action);

  /**
   * Add a {@link Consumer} for handling effects of a given type. The consumer will be invoked with
   * each incoming effect object that extends the {@code effectClass}. This is useful for cases when
   * the effect has data that is needed by the effect handler, but no effect feedback events will be
   * generated.
   *
   * <p>Adding handlers for two effect classes where one is a super-class of the other is considered
   * a collision and is not allowed. Registering the same class twice is also considered a
   * collision.
   *
   * @param effectClass the effect class to handle
   * @param consumer the effect handler for the given effect class
   * @param <G> the effect class as a type parameter
   * @return this builder
   * @throws IllegalArgumentException if there is a handler collision
   */
  <G extends F> EffectRouterBuilder<F, E> addConsumer(Class<G> effectClass, Consumer<G> consumer);

  /**
   * Add a {@link Function} for handling effects of a given type. The function will be applied to
   * each incoming effect object that extends the {@code effectClass} and the resulting event will
   * be emitted. This is useful for cases when each effect will always lead to one effect feedback
   * event.
   *
   * <p>Adding handlers for two effect classes where one is a super-class of the other is considered
   * a collision and is not allowed. Registering the same class twice is also considered a
   * collision.
   *
   * @param effectClass the effect class to handle
   * @param function the effect handler for the given effect class
   * @param <G> the effect class as a type parameter
   * @return this builder
   * @throws IllegalArgumentException if there is a handler collision
   */
  <G extends F> EffectRouterBuilder<F, E> addFunction(
      Class<G> effectClass, Function<G, E> function);

  /**
   * Add a {@link Connectable} for handling effects of a given type. Each incoming effect object
   * that extends the {@code effectClass} will be sent to the connectable. This option gives the
   * full power of {@link Connectable} to the effect handler, allowing events to be decoupled from
   * effects, meaning that effects may lead to zero or more effect feedback events, and even that
   * events may be generated without a directly triggering effect.
   *
   * <p>Adding handlers for two effect classes where one is a super-class of the other is considered
   * a collision and is not allowed. Registering the same class twice is also considered a
   * collision.
   *
   * @param effectClass the effect class to handle
   * @param connectable the effect handler for the given effect class
   * @param <G> the effect class as a type parameter
   * @return this builder
   * @throws IllegalArgumentException if there is a handler collision
   */
  <G extends F> EffectRouterBuilder<F, E> addConnectable(
      Class<G> effectClass, Connectable<G, E> connectable);

  /**
   * Optionally set a shared error handler in case a handler throws an uncaught exception.
   *
   * <p>The default is to wrap the exception and value in a {@link ConnectionException} (and
   * probably crash). Note that any exception thrown by a handler is a fatal error and this method
   * doesn't enable safe error handling, only configurable crash reporting.
   *
   * @param errorHandler a {@link BiConsumer} that will receive the effect and throwable from a
   *     failure.
   */
  EffectRouterBuilder<F, E> withFatalErrorHandler(BiConsumer<F, Throwable> errorHandler);

  /**
   * Builds an effect router {@link Connectable} based on this configuration.
   *
   * @return a new {@link Connectable}
   */
  Connectable<F, E> build();
}
