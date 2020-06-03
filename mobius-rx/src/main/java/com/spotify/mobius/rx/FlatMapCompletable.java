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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Dispatches and executes an Action1 when upstream emits a value.
 *
 * <p>This is very similar to AsyncDoOnNext, the difference being that this transformer doesn't emit
 * any items. This means that you're free to choose what the output type will be - it doesn't matter
 * what it is as no items will be emitted.
 *
 * <p>Note: as always, be very careful with performing side-effects from inside an Observable
 * stream, it is often (but not always) a sign of having overlooked something in how you design the
 * chain. Try to push side-effects either to the beginning or the end of the chain.
 */
class FlatMapCompletable<T, R> implements Observable.Transformer<T, R> {

  private final Func1<T, Completable> func;
  @Nullable private final Scheduler scheduler;

  private FlatMapCompletable(Func1<T, Completable> func, @Nullable Scheduler scheduler) {
    this.func = func;
    this.scheduler = scheduler;
  }

  static <T, R> FlatMapCompletable<T, R> createForAction(
      final Action1<T> action, @Nullable Scheduler scheduler) {
    return create(
        new Func1<T, Completable>() {
          @Override
          public Completable call(final T t) {
            return Completable.fromAction(
                new Action0() {
                  @Override
                  public void call() {
                    action.call(t);
                  }
                });
          }
        },
        scheduler);
  }

  static <T, R> FlatMapCompletable<T, R> createForAction(final Action1<T> action) {
    return createForAction(action, null);
  }

  static <T, R> FlatMapCompletable<T, R> create(Func1<T, Completable> func) {
    return create(func, null);
  }

  static <T, R> FlatMapCompletable<T, R> create(
      Func1<T, Completable> func, @Nullable Scheduler scheduler) {
    return new FlatMapCompletable<>(checkNotNull(func), scheduler);
  }

  @Override
  public Observable<R> call(Observable<T> observable) {
    return observable.flatMap(
        new Func1<T, Observable<R>>() {
          @Override
          public Observable<R> call(final T value) {
            Completable completable = func.call(value);

            if (scheduler != null) {
              completable = completable.subscribeOn(scheduler);
            }

            return completable
                .toObservable()
                .ignoreElements()
                .map(
                    new Func1<Object, R>() {
                      @Override
                      public R call(Object ignored) {
                        // Since our upstream has ignoreElements on it, values will never ever be
                        // emitted, and therefore this function call won't actually be executed.
                        // This map is  really only present in order to cast the stream to type R.
                        // Throwing an exception in this never-to-be-executed function allows us
                        // say that the return type is T without actually needing to be able to
                        // produce values of type T.
                        throw new IllegalStateException(
                            "Impossible state! ignoreElements() mustn't allow values to be emitted!");
                      }
                    });
          }
        });
  }
}
