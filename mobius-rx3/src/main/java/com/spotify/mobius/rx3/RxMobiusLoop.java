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

import com.spotify.mobius.MobiusLoop;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.functions.Consumer;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wraps a MobiusLoop into an observable transformer.
 *
 * <p>Compose it on top of an event of streams to convert it into a stream of models.
 */
class RxMobiusLoop<E, M, F> implements ObservableTransformer<E, M> {

  @Nonnull private final MobiusLoop.Factory<M, E, F> loopFactory;
  @Nonnull private final M startModel;
  @Nullable private final Set<F> startEffects;

  RxMobiusLoop(
      MobiusLoop.Factory<M, E, F> loopFactory, M startModel, @Nullable Set<F> startEffects) {
    this.loopFactory = loopFactory;
    this.startModel = startModel;
    this.startEffects = startEffects;
  }

  @Override
  public ObservableSource<M> apply(@NonNull Observable<E> upstream) {
    return Observable.create(
        new ObservableOnSubscribe<M>() {
          @Override
          public void subscribe(@NonNull ObservableEmitter<M> emitter) throws Throwable {
            final MobiusLoop<M, E, ?> loop;

            if (startEffects == null) {
              loop = loopFactory.startFrom(startModel);
            } else {
              loop = loopFactory.startFrom(startModel, startEffects);
            }

            loop.observe(
                new com.spotify.mobius.functions.Consumer<M>() {
                  @Override
                  public void accept(M value) {
                    emitter.onNext(value);
                  }
                });
            final Disposable eventsDisposable =
                upstream.subscribe(
                    new Consumer<E>() {
                      @Override
                      public void accept(E event) throws Throwable {
                        loop.dispatchEvent(event);
                      }
                    },
                    new Consumer<Throwable>() {
                      @Override
                      public void accept(Throwable throwable) throws Throwable {
                        emitter.onError(new UnrecoverableIncomingException(throwable));
                      }
                    });
            emitter.setCancellable(
                new Cancellable() {
                  @Override
                  public void cancel() throws Throwable {
                    loop.dispose();
                    eventsDisposable.dispose();
                  }
                });
          }
        });
  }
}
