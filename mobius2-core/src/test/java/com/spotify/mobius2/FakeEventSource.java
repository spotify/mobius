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
package com.spotify.mobius2;

import com.spotify.mobius2.disposables.Disposable;
import com.spotify.mobius2.functions.Consumer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

class FakeEventSource<E> implements EventSource<E> {

  private final List<Consumer<E>> myConsumers = new ArrayList<>();

  void emit(E toEmit) {
    for (Consumer<E> myConsumer : myConsumers) {
      myConsumer.accept(toEmit);
    }
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<E> eventConsumer) {
    myConsumers.add(eventConsumer);

    return new Disposable() {
      @Override
      public void dispose() {
        // no-op for now; add a 'disposed' flag or something if needed later
      }
    };
  }
}
