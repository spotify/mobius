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
package com.spotify.mobius.rx2;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.ConnectionException;
import com.spotify.mobius.Mobius;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.Update;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import java.util.HashMap;
import java.util.Map;

/** Factory methods for wrapping Mobius core classes in observable tranformers. */
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
    return new RxMobiusLoop<>(loopFactory, startModel);
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
   * Create an {@link SubtypeEffectHandlerBuilder} for handling effects based on their type.
   *
   * @param <F> the effect type
   * @param <E> the event type
   */
  public static <F, E> SubtypeEffectHandlerBuilder<F, E> subtypeEffectHandler() {
    return new SubtypeEffectHandlerBuilder<>();
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
    private OnErrorFunction<ObservableTransformer<? extends F, E>, Consumer<Throwable>>
        onErrorFunction =
            new OnErrorFunction<ObservableTransformer<? extends F, E>, Consumer<Throwable>>() {
              @Override
              public Consumer<Throwable> apply(
                  ObservableTransformer<? extends F, E> effectHandler) {
                return defaultOnError(effectHandler);
              }
            };

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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final ObservableTransformer<G, E> effectHandler) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
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
          new ObservableTransformer<F, E>() {
            @Override
            public Observable<E> apply(Observable<F> effects) {
              return effects
                  .ofType(effectClass)
                  .compose(effectHandler)
                  .doOnError(onErrorFunction.apply(effectHandler));
            }
          });

      return this;
    }

    /**
     * Add an {@link Action} for handling effects of a given type. The action will be invoked once
     * for every received effect object that extend the given class.
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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final Action action) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(action);

      return add(effectClass, Transformers.<G, E>fromAction(action));
    }

    /**
     * Add an {@link Action} for handling effects of a given type. The action will be invoked once
     * for every received effect object that extend the given class.
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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final Action action, Scheduler scheduler) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(action);

      return add(effectClass, Transformers.<G, E>fromAction(action, scheduler));
    }

    /**
     * Add an {@link Consumer} for handling effects of a given type. The consumer will be invoked
     * once for every received effect object that extend the given class.
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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final Consumer<G> consumer) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(consumer);

      return add(effectClass, Transformers.<G, E>fromConsumer(consumer));
    }

    /**
     * Add an {@link Consumer} for handling effects of a given type. The consumer will be invoked
     * once for every received effect object that extend the given class.
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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final Consumer<G> consumer, Scheduler scheduler) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(consumer);

      return add(effectClass, Transformers.<G, E>fromConsumer(consumer, scheduler));
    }

    /**
     * Add a {@link Function} for handling effects of a given type. The function will be invoked
     * once for every received effect object that extend the given class. The returned event will be
     * forwarded to the Mobius loop.
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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final Function<G, E> function) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(function);

      return add(effectClass, Transformers.fromFunction(function));
    }

    /**
     * Add a {@link Function} for handling effects of a given type. The function will be invoked
     * once for every received effect object that extend the given class. The returned event will be
     * forwarded to the Mobius loop.
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
    public <G extends F> SubtypeEffectHandlerBuilder<F, E> add(
        final Class<G> effectClass, final Function<G, E> function, Scheduler scheduler) {
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(effectClass);
      //noinspection ResultOfMethodCallIgnored
      checkNotNull(function);

      return add(effectClass, Transformers.fromFunction(function, scheduler));
    }

    /**
     * Optionally set a shared error handler in case a handler throws an uncaught exception.
     *
     * <p>The default is to use {@link RxJavaPlugins#getErrorHandler()}. Note that any exception
     * thrown by a handler is a fatal error and this method doesn't enable safe error handling, only
     * configurable crash reporting.
     *
     * @param function a function that gets told which sub-transformer failed and should return an
     *     appropriate handler for exceptions thrown.
     */
    public SubtypeEffectHandlerBuilder<F, E> withFatalErrorHandler(
        final Function<ObservableTransformer<? extends F, E>, Consumer<Throwable>> function) {
      checkNotNull(function);

      this.onErrorFunction =
          new OnErrorFunction<ObservableTransformer<? extends F, E>, Consumer<Throwable>>() {
            @Override
            public Consumer<Throwable> apply(ObservableTransformer<? extends F, E> effectHandler) {
              try {
                return function.apply(effectHandler);
              } catch (Exception e) {
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
        public void accept(Throwable throwable) throws Exception {
          RxJavaPlugins.getErrorHandler()
              .accept(new ConnectionException(effectHandler.getClass().toString(), throwable));
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
