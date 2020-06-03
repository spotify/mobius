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

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Supplier;

/** Transformers. */
final class Transformers {

  private Transformers() {}

  /**
   * Creates an {@link ObservableTransformer} that will flatten the provided {@link Action} into the
   * stream as a {@link Completable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the provided Action every time an effect is dispatched
   * to the created effect transformer.
   *
   * @param doEffect the {@link Action} to be run every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return an {@link ObservableTransformer} that can be used with a {@link
   *     RxMobius.SubtypeEffectHandlerBuilder}.
   */
  static <F, E> ObservableTransformer<F, E> fromAction(@NonNull final Action doEffect) {
    return fromAction(doEffect, null);
  }

  /**
   * Creates an {@link ObservableTransformer} that will flatten the provided {@link Action} into the
   * stream as a {@link Completable} every time it receives an effect from the upstream effects
   * observable. This Completable will be subscribed on the specified {@link Scheduler}. This will
   * result in calling the provided Action on the specified scheduler every time an effect is
   * dispatched to the created effect transformer.
   *
   * @param doEffect the {@link Action} to be run every time the effect is requested
   * @param scheduler the {@link Scheduler} that the action should be run on
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return an {@link ObservableTransformer} that can be used with a {@link
   *     RxMobius.SubtypeEffectHandlerBuilder}.
   */
  static <F, E> ObservableTransformer<F, E> fromAction(
      final Action doEffect, @Nullable final Scheduler scheduler) {
    return new ObservableTransformer<F, E>() {
      @Override
      public ObservableSource<E> apply(Observable<F> effectStream) {
        return effectStream
            .flatMapCompletable(
                new Function<F, CompletableSource>() {
                  @Override
                  public CompletableSource apply(F f) throws Exception {
                    return scheduler == null
                        ? Completable.fromAction(doEffect)
                        : Completable.fromAction(doEffect).subscribeOn(scheduler);
                  }
                })
            .toObservable();
      }
    };
  }

  /**
   * Creates an {@link ObservableTransformer} that will flatten the provided {@link Consumer} into
   * the stream as a {@link Completable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the consumer and and passing it the requested effect
   * object.
   *
   * @param doEffect the {@link Consumer} to be run every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return an {@link ObservableTransformer} that can be used with a {@link
   *     RxMobius.SubtypeEffectHandlerBuilder}.
   */
  static <F, E> ObservableTransformer<F, E> fromConsumer(final Consumer<F> doEffect) {
    return fromConsumer(doEffect, null);
  }

  /**
   * Creates an {@link ObservableTransformer} that will flatten the provided {@link Consumer} into
   * the stream as a {@link Completable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the consumer on the specified scheduler, and passing it
   * the requested effect object.
   *
   * @param doEffect the {@link Consumer} to be run every time the effect is requested
   * @param scheduler the {@link Scheduler} to be used when invoking the consumer
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return an {@link ObservableTransformer} that can be used with a {@link
   *     RxMobius.SubtypeEffectHandlerBuilder}.
   */
  static <F, E> ObservableTransformer<F, E> fromConsumer(
      final Consumer<F> doEffect, @Nullable final Scheduler scheduler) {
    return new ObservableTransformer<F, E>() {
      @Override
      public ObservableSource<E> apply(Observable<F> effectStream) {
        return effectStream
            .flatMapCompletable(
                new Function<F, CompletableSource>() {
                  @Override
                  public CompletableSource apply(final F effect) {
                    Completable completable =
                        Completable.fromAction(
                            new Action() {
                              @Override
                              public void run() throws Throwable {
                                doEffect.accept(effect);
                              }
                            });
                    return scheduler == null ? completable : completable.subscribeOn(scheduler);
                  }
                })
            .toObservable();
      }
    };
  }

  /**
   * Creates an {@link ObservableTransformer} that will flatten the provided {@link Function} into
   * the stream as an {@link Observable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the function on the specified scheduler, and passing it
   * the requested effect object then emitting its returned value.
   *
   * @param function the {@link Function} to be invoked every time the effect is requested
   * @param scheduler the {@link Scheduler} to be used when invoking the function
   * @param <F> the type of Effect this transformer handles
   * @param <E> the type of Event this transformer emits
   * @return an {@link ObservableTransformer} that can be used with a {@link
   *     RxMobius.SubtypeEffectHandlerBuilder}.
   */
  static <F, E> ObservableTransformer<F, E> fromFunction(
      final Function<F, E> function, @Nullable final Scheduler scheduler) {
    return new ObservableTransformer<F, E>() {
      @Override
      public ObservableSource<E> apply(Observable<F> effectStream) {
        return effectStream.flatMap(
            new Function<F, ObservableSource<E>>() {
              @Override
              public ObservableSource<E> apply(@NonNull F f) {
                Observable<E> eventObservable =
                    Observable.fromSupplier(
                        new Supplier<E>() {
                          @Override
                          public E get() throws Throwable {
                            return function.apply(f);
                          }
                        });
                return scheduler == null ? eventObservable : eventObservable.subscribeOn(scheduler);
              }
            });
      }
    };
  }

  /**
   * Creates an {@link ObservableTransformer} that will flatten the provided {@link Function} into
   * the stream as an {@link Observable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the function on the immediate scheduler, and passing it
   * the requested effect object then emitting its returned value.
   *
   * @param function {@link Function} to be invoked every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> the type of Event this transformer emits
   * @return an {@link ObservableTransformer} that can be used with a {@link
   *     RxMobius.SubtypeEffectHandlerBuilder}.
   */
  static <F, E> ObservableTransformer<F, E> fromFunction(final Function<F, E> function) {
    return fromFunction(function, null);
  }
}
