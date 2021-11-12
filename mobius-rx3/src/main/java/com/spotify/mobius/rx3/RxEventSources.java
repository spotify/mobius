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

import com.spotify.mobius.EventSource;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.atomic.AtomicBoolean;
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
        final AtomicBoolean disposed = new AtomicBoolean();
        final Disposable disposable =
            eventSource.subscribe(
                value -> {
                  synchronized (disposed) {
                    if (!disposed.get()) {
                      eventConsumer.accept(value);
                    }
                  }
                });
        return () -> {
          synchronized (disposed) {
            disposable.dispose();
            disposed.set(true);
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
        emitter -> {
          final com.spotify.mobius.disposables.Disposable disposable =
              eventSource.subscribe(emitter::onNext);
          emitter.setCancellable(disposable::dispose);
        });
  }
}
