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

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.plugins.RxJavaHooks;

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
   * @param observables the observables you want to include in this event source
   * @param <E> the event type
   * @return an EventSource based on the provided observables
   */
  @SafeVarargs
  public static <E> EventSource<E> fromObservables(Observable<E>... observables) {
    final Observable<E> eventSource = Observable.merge(observables);

    return new EventSource<E>() {
      @Nonnull
      @Override
      public Disposable subscribe(final Consumer<E> eventConsumer) {
        final AtomicBoolean disposed = new AtomicBoolean();
        final Subscription subscription =
            eventSource.subscribe(
                value -> {
                  synchronized (disposed) {
                    if (!disposed.get()) {
                      eventConsumer.accept(value);
                    }
                  }
                },
                RxJavaHooks::onError);
        return () -> {
          synchronized (disposed) {
            subscription.unsubscribe();
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
  public static <E> Observable<E> toObservable(
      final EventSource<E> eventSource, BackpressureMode backpressureMode) {
    checkNotNull(eventSource);
    checkNotNull(backpressureMode);

    return Observable.create(
        new Action1<Emitter<E>>() {
          @Override
          public void call(final Emitter<E> emitter) {
            final Disposable disposable =
                eventSource.subscribe(
                    new Consumer<E>() {
                      @Override
                      public void accept(E value) {
                        emitter.onNext(value);
                      }
                    });

            emitter.setCancellation(
                new Cancellable() {
                  @Override
                  public void cancel() throws Exception {
                    disposable.dispose();
                  }
                });
          }
        },
        backpressureMode);
  }
}
