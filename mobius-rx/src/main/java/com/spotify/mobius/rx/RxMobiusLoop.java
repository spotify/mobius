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

import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.functions.Consumer;
import java.util.Set;
import javax.annotation.Nullable;
import rx.Emitter;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;

/**
 * Wraps a MobiusLoop into an observable transformer.
 *
 * <p>Compose it on top of an event of streams to convert it into a stream of models.
 */
class RxMobiusLoop<E, M, F> implements Observable.Transformer<E, M> {

  private final MobiusLoop.Factory<M, E, F> loopFactory;
  private final M startModel;
  @Nullable private final Set<F> startEffects;

  RxMobiusLoop(MobiusLoop.Factory<M, E, F> loopFactory, M loopStart, @Nullable Set<F> effects) {
    this.loopFactory = loopFactory;
    this.startModel = loopStart;
    this.startEffects = effects;
  }

  @Override
  public Observable<M> call(final Observable<E> events) {
    return Observable.create(
        new Action1<Emitter<M>>() {
          @Override
          public void call(final Emitter<M> emitter) {
            final MobiusLoop<M, E, ?> loop;

            if (startEffects == null) {
              loop = loopFactory.startFrom(startModel);
            } else {
              loop = loopFactory.startFrom(startModel, startEffects);
            }

            loop.observe(
                new Consumer<M>() {
                  @Override
                  public void accept(M newModel) {
                    emitter.onNext(newModel);
                  }
                });

            final Subscription eventSubscription =
                events.subscribe(
                    new Observer<E>() {
                      @Override
                      public void onCompleted() {
                        // TODO: complain loudly! shouldn't ever complete
                      }

                      @Override
                      public void onError(Throwable e) {
                        emitter.onError(new UnrecoverableIncomingException(e));
                      }

                      @Override
                      public void onNext(E event) {
                        loop.dispatchEvent(event);
                      }
                    });

            emitter.setCancellation(
                new Cancellable() {
                  @Override
                  public void cancel() throws Exception {
                    loop.dispose();
                    eventSubscription.unsubscribe();
                  }
                });
          }
        },
        Emitter.BackpressureMode.NONE);
  }
}
