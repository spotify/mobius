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

import com.spotify.mobius.ConnectionException;
import com.spotify.mobius.Mobius;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.Update;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Factory methods for wrapping Mobius core classes in observable transformers. */
public final class RxMobius {

  private RxMobius() {
    // prevent instantiation
  }

  /**
   * Create an observable transformer that starts from a given model.
   *
   * <p>Every time the resulting observable is subscribed to, a new MobiusLoop will be started from
   * the given model.
   *
   * @param loopFactory gets invoked for each subscription, to create a new MobiusLoop instance
   * @param startModel the starting point for each new loop
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   * @return a transformer from event to model that you can connect to your UI
   */
  public static <M, E, F> ObservableTransformer<E, M> loopFrom(
      final MobiusLoop.Factory<M, E, F> loopFactory, final M startModel) {
    return new RxMobiusLoop<>(loopFactory, startModel, null);
  }

  /**
   * Create an observable transformer that starts from a given model and given effects.
   *
   * <p>Every time the resulting observable is subscribed to, a new MobiusLoop will be started from
   * the given model and the given effects.
   *
   * @param loopFactory gets invoked for each subscription, to create a new MobiusLoop instance
   * @param startModel the starting point for each new loop
   * @param startEffects the starting effects for each new loop
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   * @return a transformer from event to model that you can connect to your UI
   */
  public static <M, E, F> ObservableTransformer<E, M> loopFrom(
      final MobiusLoop.Factory<M, E, F> loopFactory,
      final M startModel,
      final Set<F> startEffects) {
    return new RxMobiusLoop<>(loopFactory, startModel, startEffects);
  }

  /**
   * Create a {@link MobiusLoop.Builder} to help you configure a MobiusLoop before starting it.
   *
   * <p>Once done configuring the loop you can start the loop using {@link
   * MobiusLoop.Factory#startFrom(Object)}.
   *
   * @param update the {@link Update} function of the loop
   * @param effectHandler the {@link ObservableTransformer} effect handler of the loop
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   * @return a {@link MobiusLoop.Builder} instance that you can further configure before starting
   *     the loop
   */
  public static <M, E, F> MobiusLoop.Builder<M, E, F> loop(
      Update<M, E, F> update, ObservableTransformer<F, E> effectHandler) {
    return Mobius.loop(update, RxConnectables.fromTransformer(effectHandler));
  }

  /**
   * Create an {@link RxMobius.SubtypeEffectHandlerBuilder} for handling effects based on their
   * type.
   *
   * @param <F> the effect type
   * @param <E> the event type
   */
  public static <F, E> RxMobius.SubtypeEffectHandlerBuilder<F, E> subtypeEffectHandler() {
    return new RxMobius.SubtypeEffectHandlerBuilder<>();
  }

  /**
   * Builder for a type-routing effect handler.
   *
   * <p>Register handlers for different subtypes of F using the add(...) methods, and call {@link
   * #build()} to create an instance of the effect handler. You can then create a loop with the
   * router as the effect handler using {@link #loop(Update, ObservableTransformer)}.
   *
   * <p>The handler will look at the type of each incoming effect object and try to find a
   * registered handler for that particular subtype of F. If a handler is found, it will be given
   * the effect object, otherwise an exception will be thrown.
   *
   * <p>All the classes that the effect router know about must have a common type F. Note that
   * instances of the builder are mutable and not thread-safe.
   */
  public static class SubtypeEffectHandlerBuilder<F, E> {

    private final Map<Class<?>, ObservableTransformer<F, E>> effectPerformerMap = new HashMap<>();
    private SubtypeEffectHandlerBuilder.OnErrorFunction<
            ObservableTransformer<? extends F, E>, Consumer<Throwable>>
        onErrorFunction = SubtypeEffectHandlerBuilder::defaultOnError;

    private SubtypeEffectHandlerBuilder() {}

    /**
     * Add an {@link ObservableTransformer} for handling effects of a given type. The handler will
     * receive all effect objects that extend the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param effectHandler the effect handler for the given effect class
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addTransformer(
        final Class<G> effectClass, final ObservableTransformer<G, E> effectHandler) {
      checkNotNull(effectClass);
      checkNotNull(effectHandler);

      for (Class<?> cls : effectPerformerMap.keySet()) {
        if (cls.isAssignableFrom(effectClass) || effectClass.isAssignableFrom(cls)) {
          throw new IllegalArgumentException(
              "Effect classes may not be assignable to each other, collision found: "
                  + effectClass.getSimpleName()
                  + " <-> "
                  + cls.getSimpleName());
        }
      }

      effectPerformerMap.put(
          effectClass,
          (Observable<F> effects) ->
              effects
                  .ofType(effectClass)
                  .compose(effectHandler)
                  .doOnError(onErrorFunction.apply(effectHandler)));

      return this;
    }

    /**
     * Add a {@link Function} for handling effects of a given type. The function will be invoked
     * once for every received effect object that extends the given class. The returned event will
     * be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param function the function that should be invoked for the effect
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addFunction(
        final Class<G> effectClass, final Function<G, E> function) {
      checkNotNull(effectClass);
      checkNotNull(function);
      return addTransformer(effectClass, Transformers.fromFunction(function));
    }

    /**
     * Add a {@link Function} for handling effects of a given type. The function will be invoked
     * once for every received effect object that extends the given class. The returned event will
     * be forwarded to the Mobius loop.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param function the function that should be invoked for the effect
     * @param scheduler the scheduler that should be used when invoking the function
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addFunction(
        final Class<G> effectClass, final Function<G, E> function, Scheduler scheduler) {
      checkNotNull(effectClass);
      checkNotNull(function);
      return addTransformer(effectClass, Transformers.fromFunction(function, scheduler));
    }

    /**
     * Add an {@link Action} for handling effects of a given type. The action will be invoked once
     * for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param action the action that should be invoked for the effect
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addAction(
        final Class<G> effectClass, final Action action) {
      checkNotNull(effectClass);
      checkNotNull(action);
      return addTransformer(effectClass, Transformers.fromAction(action));
    }

    /**
     * Add an {@link Action} for handling effects of a given type. The action will be invoked once
     * for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param action the action that should be invoked for the effect
     * @param scheduler the scheduler that should be used to invoke the action
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addAction(
        final Class<G> effectClass, final Action action, Scheduler scheduler) {
      checkNotNull(effectClass);
      checkNotNull(action);
      return addTransformer(effectClass, Transformers.fromAction(action, scheduler));
    }

    /**
     * Add an {@link Consumer} for handling effects of a given type. The consumer will be invoked
     * once for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param consumer the consumer that should be invoked for the effect
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addConsumer(
        final Class<G> effectClass, final Consumer<G> consumer) {
      checkNotNull(effectClass);
      checkNotNull(consumer);
      return addTransformer(effectClass, Transformers.fromConsumer(consumer));
    }

    /**
     * Add an {@link Consumer} for handling effects of a given type. The consumer will be invoked
     * once for every received effect object that extends the given class.
     *
     * <p>Adding handlers for two effect classes where one is a super-class of the other is
     * considered a collision and is not allowed. Registering the same class twice is also
     * considered a collision.
     *
     * @param effectClass the class to handle
     * @param consumer the consumer that should be invoked for the effect
     * @param scheduler the scheduler that should be used to invoke the consumer
     * @param <G> the effect class as a type parameter
     * @return this builder
     * @throws IllegalArgumentException if there is a handler collision
     */
    public <G extends F> RxMobius.SubtypeEffectHandlerBuilder<F, E> addConsumer(
        final Class<G> effectClass, final Consumer<G> consumer, Scheduler scheduler) {
      checkNotNull(effectClass);
      checkNotNull(consumer);
      return addTransformer(effectClass, Transformers.fromConsumer(consumer, scheduler));
    }

    /**
     * Optionally set a shared error handler in case a handler throws an uncaught exception.
     *
     * <p>The default is to use {@link RxJavaPlugins#onError(Throwable)}. Note that any exception
     * thrown by a handler is a fatal error and this method doesn't enable safe error handling, only
     * configurable crash reporting.
     *
     * @param function a function that gets told which sub-transformer failed and should return an
     *     appropriate handler for exceptions thrown.
     */
    public RxMobius.SubtypeEffectHandlerBuilder<F, E> withFatalErrorHandler(
        final Function<ObservableTransformer<? extends F, E>, Consumer<Throwable>> function) {
      checkNotNull(function);
      this.onErrorFunction =
          new OnErrorFunction<ObservableTransformer<? extends F, E>, Consumer<Throwable>>() {
            @Override
            public Consumer<Throwable> apply(ObservableTransformer<? extends F, E> effectHandler) {
              try {
                return function.apply(effectHandler);
              } catch (Throwable e) {
                throw new RuntimeException(
                    "FATAL: fatal error handler threw exception for effect handler: "
                        + effectHandler,
                    e);
              }
            }
          };

      return this;
    }

    public ObservableTransformer<F, E> build() {
      return new MobiusEffectRouter<>(effectPerformerMap.keySet(), effectPerformerMap.values());
    }

    private static <F, E> Consumer<Throwable> defaultOnError(
        final ObservableTransformer<? extends F, E> effectHandler) {
      return new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) throws Throwable {
          RxJavaPlugins.onError(
              new ConnectionException(
                  "in effect handler: " + effectHandler.getClass().toString(), throwable));
        }
      };
    }

    private interface OnErrorFunction<T, R> extends Function<T, R> {
      // override in order to remove 'throws Exception'
      @Override
      R apply(T t);
    }
  }
}
