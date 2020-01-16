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
package com.spotify.mobius.android;

import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Accumulator<T> {
  private final List<T> events;
  private final AtomicBoolean handled = new AtomicBoolean(false);

  Accumulator() {
    events = new ArrayList<>();
  }

  Accumulator(T event) {
    final List<T> newEvents = new ArrayList<>(1);
    newEvents.add(event);
    events = newEvents;
  }

  @Nonnull
  static <T> Accumulator<T> add(@Nullable Accumulator<T> accumulator, @Nonnull T event) {
    if (accumulator == null) {
      return new Accumulator<>(event);
    } else {
      return accumulator.plus(event);
    }
  }

  void handle(@Nonnull Consumer<T> eventConsumer) {
    if (handled.compareAndSet(false, true)) {
      synchronized (events) {
        for (T event : events) {
          eventConsumer.accept(event);
        }
      }
    }
  }

  @Nonnull
  private Accumulator<T> plus(@Nonnull T event) {
    if (handled.get()) {
      return new Accumulator<>(event);
    } else {
      synchronized (events) {
        events.add(event);
      }
      return this;
    }
  }
}
