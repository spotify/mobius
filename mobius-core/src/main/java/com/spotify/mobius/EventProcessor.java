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
package com.spotify.mobius;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes events and emits effects and models as a result of that.
 *
 * @param <M> model type
 * @param <E> event type
 * @param <F> effect descriptor type
 */
class EventProcessor<M, E, F> {

  private final MobiusStore<M, E, F> store;
  private final Consumer<F> effectConsumer;
  private final Consumer<M> modelConsumer;

  // concurrency note: the two below fields are only read and written in synchronized sections,
  // hence no need for further coordination.
  private final List<E> eventsReceivedBeforeInit = new ArrayList<>();
  private boolean initialised = false;

  EventProcessor(
      MobiusStore<M, E, F> store, Consumer<F> effectConsumer, Consumer<M> modelConsumer) {
    this.store = checkNotNull(store);
    this.effectConsumer = checkNotNull(effectConsumer);
    this.modelConsumer = checkNotNull(modelConsumer);
  }

  synchronized void init(Iterable<F> startEffects) {
    if (initialised) {
      throw new IllegalStateException("already initialised");
    }

    dispatchModel(store.model());
    dispatchEffects(startEffects);

    initialised = true;
    for (E event : eventsReceivedBeforeInit) {
      update(event);
    }
  }

  synchronized void update(E event) {
    if (!initialised) {
      eventsReceivedBeforeInit.add(event);
      return;
    }

    Next<M, F> next = store.update(event);

    next.ifHasModel(
        new Consumer<M>() {
          @Override
          public void accept(M model) {
            dispatchModel(model);
          }
        });
    dispatchEffects(next.effects());
  }

  private void dispatchModel(M model) {
    modelConsumer.accept(model);
  }

  private void dispatchEffects(Iterable<F> effects) {
    for (F effect : effects) {
      effectConsumer.accept(effect);
    }
  }

  /**
   * Factory for event processors.
   *
   * @param <M> model type
   * @param <E> event type
   * @param <F> effect descriptor type
   */
  static class Factory<M, E, F> {

    private final MobiusStore<M, E, F> store;

    Factory(MobiusStore<M, E, F> store) {
      this.store = checkNotNull(store);
    }

    public EventProcessor<M, E, F> create(Consumer<F> effectConsumer, Consumer<M> modelConsumer) {
      return new EventProcessor<>(store, checkNotNull(effectConsumer), checkNotNull(modelConsumer));
    }
  }
}
