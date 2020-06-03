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

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;
import rx.Emitter;
import rx.Emitter.BackpressureMode;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.plugins.RxJavaHooks;
import rx.subjects.PublishSubject;

/**
 * Contains utility methods for converting back and forth between {@link rx.Observable.Transformer}s
 * and {@link Connectable}s.
 */
public final class RxConnectables {
  private RxConnectables() {}

  public static <I, O> Connectable<I, O> fromTransformer(
      final Observable.Transformer<I, O> transformer) {
    checkNotNull(transformer);

    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(final Consumer<O> output) {
        final PublishSubject<I> subject = PublishSubject.create();

        final Subscription subscription =
            subject
                .compose(transformer)
                .subscribe(
                    new Observer<O>() {
                      @Override
                      public void onCompleted() {
                        // TODO: complain loudly! shouldn't ever complete
                      }

                      @Override
                      public void onError(Throwable e) {
                        RxJavaHooks.onError(e);
                      }

                      @Override
                      public void onNext(O e) {
                        output.accept(e);
                      }
                    });

        return new Connection<I>() {
          @Override
          public void accept(I effect) {
            if (subscription.isUnsubscribed())
              throw new IllegalStateException(
                  "Effect handlers cannot perform effects after they've been disposed of");
            subject.onNext(effect);
          }

          @Override
          public void dispose() {
            subscription.unsubscribe();
          }
        };
      }
    };
  }

  public static <I, O> Observable.Transformer<I, O> toTransformer(
      final Connectable<I, O> connectable, final BackpressureMode backpressureMode) {
    return new Observable.Transformer<I, O>() {
      @Override
      public Observable<O> call(final Observable<I> upstream) {
        return Observable.create(
            new Action1<Emitter<O>>() {
              @Override
              public void call(final Emitter<O> emitter) {
                Consumer<O> output =
                    new Consumer<O>() {
                      @Override
                      public void accept(O value) {
                        emitter.onNext(value);
                      }
                    };

                final Connection<I> input = connectable.connect(output);
                final Subscription subscription =
                    upstream.subscribe(
                        new Action1<I>() {
                          @Override
                          public void call(I f) {
                            input.accept(f);
                          }
                        },
                        new Action1<Throwable>() {
                          @Override
                          public void call(Throwable throwable) {
                            emitter.onError(throwable);
                          }
                        },
                        new Action0() {
                          @Override
                          public void call() {
                            emitter.onCompleted();
                          }
                        });

                emitter.setCancellation(
                    new Cancellable() {
                      @Override
                      public void cancel() throws Exception {
                        subscription.unsubscribe();
                        input.dispose();
                      }
                    });
              }
            },
            backpressureMode);
      }
    };
  }

  public static <I, O> Observable.Transformer<I, O> toTransformer(
      final Connectable<I, O> connectable) {
    return toTransformer(connectable, BackpressureMode.NONE);
  }
}
