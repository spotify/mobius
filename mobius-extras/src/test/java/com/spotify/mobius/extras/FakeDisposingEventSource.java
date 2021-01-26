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
package com.spotify.mobius.extras;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

class FakeDisposingEventSource<E> implements EventSource<E> {

  private final List<Consumer<E>> myConsumers = new CopyOnWriteArrayList<>();

  public void assertConsumerCount(int numConsumers) {
    assertThat(myConsumers.size(), equalTo(numConsumers));
  }

  void emit(E toEmit) {
    for (Consumer<E> myConsumer : myConsumers) {
      myConsumer.accept(toEmit);
    }
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<E> eventConsumer) {
    myConsumers.add(eventConsumer);
    return () -> myConsumers.remove(eventConsumer);
  }
}
