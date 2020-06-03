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

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import javax.annotation.Nonnull;

/**
 * Contains utility methods for converting back and forth between {@link ObservableTransformer}s and
 * {@link Connectable}s.
 */
public final class RxConnectables {

  private RxConnectables() {}

  public static <I, O> Connectable<I, O> fromTransformer(
      @NonNull final ObservableTransformer<I, O> transformer) {
    checkNotNull(transformer);
    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(com.spotify.mobius.functions.Consumer<O> output) {
        final PublishSubject<I> subject = PublishSubject.create();

        final Disposable disposable =
            subject
                .compose(transformer)
                .subscribe(
                    new Consumer<O>() {
                      @Override
                      public void accept(O value) throws Throwable {
                        output.accept(value);
                      }
                    },
                    new Consumer<Throwable>() {
                      @Override
                      public void accept(Throwable error) throws Throwable {
                        RxJavaPlugins.onError(error);
                      }
                    },
                    new Action() {
                      @Override
                      public void run() throws Throwable {}
                    });

        return new Connection<I>() {
          public void accept(I effect) {
            subject.onNext(effect);
          }

          @Override
          public void dispose() {
            disposable.dispose();
          }
        };
      }
    };
  }

  @NonNull
  public static <I, O> ObservableTransformer<I, O> toTransformer(
      final Connectable<I, O> connectable) {
    return new ObservableTransformer<I, O>() {
      @Override
      public @NonNull ObservableSource<O> apply(@NonNull Observable<I> upstream) {
        return Observable.create(
            new ObservableOnSubscribe<O>() {
              @Override
              public void subscribe(@NonNull ObservableEmitter<O> emitter) throws Throwable {
                com.spotify.mobius.functions.Consumer<O> output = emitter::onNext;
                final Connection<I> input = connectable.connect(output);
                final Disposable disposable =
                    upstream.subscribe(
                        new Consumer<I>() {
                          @Override
                          public void accept(I value) throws Throwable {
                            input.accept(value);
                          }
                        },
                        new Consumer<Throwable>() {
                          @Override
                          public void accept(Throwable error) throws Throwable {
                            emitter.onError(error);
                          }
                        },
                        new Action() {
                          @Override
                          public void run() throws Throwable {
                            emitter.onComplete();
                          }
                        });

                emitter.setCancellable(
                    new Cancellable() {
                      @Override
                      public void cancel() throws Throwable {
                        disposable.dispose();
                        input.dispose();
                      }
                    });
              }
            });
      }
    };
  }
}
