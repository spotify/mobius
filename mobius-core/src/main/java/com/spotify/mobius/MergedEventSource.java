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
package com.spotify.mobius;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * An {@link EventSource} that merges multiple sources into one
 *
 * @param <E> The type of Events the sources will emit
 */
public class MergedEventSource<E> implements EventSource<E> {
  private final List<EventSource<E>> eventSources;

  @SafeVarargs
  public static <E> EventSource<E> from(
      EventSource<E> eventSource, EventSource<E>... eventSources) {
    List<EventSource<E>> allSources = new ArrayList<>();
    allSources.add(checkNotNull(eventSource));
    for (EventSource<E> es : eventSources) {
      allSources.add(checkNotNull(es));
    }
    return new MergedEventSource<>(allSources);
  }

  private MergedEventSource(List<EventSource<E>> sources) {
    eventSources = sources;
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<E> eventConsumer) {
    final List<Disposable> disposables = new ArrayList<>(eventSources.size());
    for (EventSource<E> eventSource : eventSources) {
      disposables.add(eventSource.subscribe(eventConsumer));
    }

    return new Disposable() {
      @Override
      public void dispose() {
        for (Disposable disposable : disposables) {
          disposable.dispose();
        }
      }
    };
  }
}
