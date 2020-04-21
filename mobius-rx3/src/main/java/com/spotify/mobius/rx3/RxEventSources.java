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
package com.spotify.mobius.rx3;

import com.spotify.mobius.EventSource;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import javax.annotation.Nonnull;

/** RxEventSources. */
public final class RxEventSources {

  private RxEventSources() {}

  /**
   * Create an event source from the given RxJava streams.
   *
   * <p>All streams must be mapped to your event type.
   *
   * @param sources the observables you want to include in this event source
   * @param <E> the event type
   * @return an EventSource based on the provided observables
   */
  @SafeVarargs
  public static <E> EventSource<E> fromObservables(@NonNull final ObservableSource<E>... sources) {
    final Observable<E> eventSource = Observable.mergeArray(sources);
    return new EventSource<E>() {

      @Nonnull
      @Override
      public com.spotify.mobius.disposables.Disposable subscribe(
          com.spotify.mobius.functions.Consumer<E> eventConsumer) {
        final Disposable disposable =
            eventSource.subscribe(
                new Consumer<E>() {
                  @Override
                  public void accept(E value) throws Throwable {
                    eventConsumer.accept(value);
                  }
                },
                new Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable error) throws Throwable {
                    RxJavaPlugins.onError(error);
                  }
                });
        return new com.spotify.mobius.disposables.Disposable() {
          @Override
          public void dispose() {
            disposable.dispose();
          }
        };
      }
    };
  }

  /**
   * Create an observable from the given event source.
   *
   * @param eventSource the eventSource you want to convert to an observable
   * @param <E> the event type
   * @return an Observable based on the provided event source
   */
  @NonNull
  public static <E> Observable<E> toObservable(final EventSource<E> eventSource) {
    return Observable.create(
        new ObservableOnSubscribe<E>() {
          @Override
          public void subscribe(@NonNull ObservableEmitter<E> emitter) throws Throwable {
            final com.spotify.mobius.disposables.Disposable disposable =
                eventSource.subscribe(
                    new com.spotify.mobius.functions.Consumer<E>() {
                      @Override
                      public void accept(E value) {
                        emitter.onNext(value);
                      }
                    });
            emitter.setCancellable(
                new Cancellable() {
                  @Override
                  public void cancel() throws Throwable {
                    disposable.dispose();
                  }
                });
          }
        });
  }
}
