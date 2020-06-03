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
package com.spotify.mobius.rx;

import com.spotify.mobius.functions.Function;
import com.spotify.mobius.rx.RxMobius.SubtypeEffectHandlerBuilder;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import rx.Completable;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Scheduler;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * A {@link Transformer} factory that creates transformers from {@link Action0} and {@link Action1}.
 * These transformers are useful as effect handlers for effects that do not result in events.
 */
class Transformers {

  private Transformers() {}

  /**
   * Creates a {@link Transformer} that will flatten the provided {@link Action0} into the stream as
   * a {@link Completable} every time it receives an effect from the upstream effects observable.
   * This will result in calling the provided Action every time an effect is dispatched to the
   * created transformer.
   *
   * @param doEffect {@link Action0} to be run every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return a {@link Transformer} that can be used with an {@link SubtypeEffectHandlerBuilder}
   */
  static <F, E> Transformer<F, E> fromAction(final Action0 doEffect) {
    return fromAction(doEffect, null);
  }

  /**
   * Creates an {@link Transformer} that will flatten the provided {@link Action0} into the stream
   * as a {@link Completable} every time it receives an effect from the upstream effects observable.
   * This Completable will be subscribed on the specified {@link Scheduler}. This will result in
   * calling the provided Action on the specified scheduler every time an effect dispatched to the
   * created effect transformer.
   *
   * @param doEffect {@link Action0} to be run every time the effect is requested
   * @param scheduler the {@link Scheduler} that the action should be run on
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return a {@link Transformer} that can be used with an {@link SubtypeEffectHandlerBuilder}
   */
  static <F, E> Transformer<F, E> fromAction(
      final Action0 doEffect, @Nullable final Scheduler scheduler) {
    return fromConsumer(
        new Action1<F>() {
          @Override
          public void call(F f) {
            try {
              doEffect.call();
            } catch (Exception e) {
              throw OnErrorThrowable.from(e);
            }
          }
        },
        scheduler);
  }

  /**
   * Creates an {@link Transformer} that will flatten the provided {@link Action1} into the stream
   * as a {@link Completable} every time it receives an effect from the upstream effects observable.
   * This will result in calling the consumer and passing it the requested effect object.
   *
   * @param doEffect {@link Action1} to be run every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return a {@link Transformer} that can be used with an {@link SubtypeEffectHandlerBuilder}
   */
  static <F, E> Transformer<F, E> fromConsumer(final Action1<F> doEffect) {
    return fromConsumer(doEffect, null);
  }

  /**
   * Creates an {@link Transformer} that will flatten the provided {@link Action1} into the stream
   * as a {@link Completable} every time it receives an effect from the upstream effects observable.
   * This will result in calling the consumer on the specified scheduler, and passing it the
   * requested effect object.
   *
   * @param doEffect {@link Action1} to be run every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> these transformers are for effects that do not result in any events; however, they
   *     still need to share the same Event type
   * @return a {@link Transformer} that can be used with an {@link SubtypeEffectHandlerBuilder}
   */
  static <F, E> Transformer<F, E> fromConsumer(
      final Action1<F> doEffect, @Nullable final Scheduler scheduler) {
    return new Transformer<F, E>() {
      @Override
      public Observable<E> call(Observable<F> effectStream) {
        return effectStream.compose(FlatMapCompletable.<F, E>createForAction(doEffect, scheduler));
      }
    };
  }

  /**
   * Creates an {@link Observable.Transformer} that will flatten the provided {@link Func1} into the
   * stream as an {@link Observable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the function on the specified scheduler, and passing it
   * the requested effect object then emitting its returned value.
   *
   * @param function the {@link Func1} to be invoked every time the effect is requested
   * @param scheduler the {@link Scheduler} to be used when invoking the function
   * @param <F> the type of Effect this transformer handles
   * @param <E> the type of Event this transformer emits
   * @return an {@link Observable.Transformer} that can be used with a {@link
   *     SubtypeEffectHandlerBuilder}.
   */
  static <F, E> Observable.Transformer<F, E> fromFunction(
      final Function<F, E> function, @Nullable final Scheduler scheduler) {
    return new Observable.Transformer<F, E>() {
      @Override
      public Observable<E> call(Observable<F> effectStream) {
        return effectStream.flatMap(
            new Func1<F, Observable<E>>() {
              @Override
              public Observable<E> call(final F f) {
                Observable<E> eventObservable =
                    Observable.fromCallable(
                        new Callable<E>() {
                          @Override
                          public E call() throws Exception {
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
   * Creates an {@link Observable.Transformer} that will flatten the provided {@link Func1} into the
   * stream as an {@link Observable} every time it receives an effect from the upstream effects
   * observable. This will result in calling the function on the immediate scheduler, and passing it
   * the requested effect object then emitting its returned value.
   *
   * @param function {@link Func1} to be invoked every time the effect is requested
   * @param <F> the type of Effect this transformer handles
   * @param <E> the type of Event this transformer emits
   * @return an {@link Observable.Transformer} that can be used with a {@link
   *     SubtypeEffectHandlerBuilder}.
   */
  static <F, E> Observable.Transformer<F, E> fromFunction(final Function<F, E> function) {
    return fromFunction(function, null);
  }
}
