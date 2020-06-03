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
package com.spotify.mobius.rx2;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Cancellable;
import io.reactivex.plugins.RxJavaPlugins;
import javax.annotation.Nonnull;

/**
 * Contains utility methods for converting back and forth between {@link Observable}s and {@link
 * EventSource}s.
 */
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
  public static <E> EventSource<E> fromObservables(ObservableSource<E>... sources) {
    final Observable<E> eventSource = Observable.mergeArray(sources);

    return new EventSource<E>() {
      @Nonnull
      @Override
      public Disposable subscribe(final Consumer<E> eventConsumer) {
        final io.reactivex.disposables.Disposable subscription =
            eventSource.subscribe(
                new io.reactivex.functions.Consumer<E>() {
                  @Override
                  public void accept(E e) throws Exception {
                    eventConsumer.accept(e);
                  }
                },
                new io.reactivex.functions.Consumer<Throwable>() {
                  @Override
                  public void accept(Throwable e) throws Exception {
                    RxJavaPlugins.onError(e);
                  }
                });

        return new Disposable() {
          @Override
          public void dispose() {
            subscription.dispose();
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
  public static <E> Observable<E> toObservable(final EventSource<E> eventSource) {
    return Observable.create(
        new ObservableOnSubscribe<E>() {
          @Override
          public void subscribe(final ObservableEmitter<E> emitter) throws Exception {
            final Disposable disposable =
                eventSource.subscribe(
                    new Consumer<E>() {
                      @Override
                      public void accept(E value) {
                        emitter.onNext(value);
                      }
                    });

            emitter.setCancellable(
                new Cancellable() {
                  @Override
                  public void cancel() throws Exception {
                    disposable.dispose();
                  }
                });
          }
        });
  }
}
