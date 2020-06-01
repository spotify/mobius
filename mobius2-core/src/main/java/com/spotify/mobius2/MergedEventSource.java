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
import com.spotify.mobius2.internal_util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * An {@link com.spotify.mobius2.EventSource} that merges multiple sources into one
 *
 * @param <E> The type of Events the sources will emit
 */
public class MergedEventSource<E> implements com.spotify.mobius2.EventSource<E> {
  private final List<com.spotify.mobius2.EventSource<E>> eventSources;

  @SafeVarargs
  public static <E> com.spotify.mobius2.EventSource<E> from(
      com.spotify.mobius2.EventSource<E> eventSource,
      com.spotify.mobius2.EventSource<E>... eventSources) {
    List<com.spotify.mobius2.EventSource<E>> allSources = new ArrayList<>();
    allSources.add(Preconditions.checkNotNull(eventSource));
    for (com.spotify.mobius2.EventSource<E> es : eventSources) {
      allSources.add(Preconditions.checkNotNull(es));
    }
    return new MergedEventSource<>(allSources);
  }

  private MergedEventSource(List<com.spotify.mobius2.EventSource<E>> sources) {
    eventSources = sources;
  }

  @Nonnull
  @Override
  public com.spotify.mobius2.disposables.Disposable subscribe(Consumer<E> eventConsumer) {
    final List<com.spotify.mobius2.disposables.Disposable> disposables =
        new ArrayList<>(eventSources.size());
    for (EventSource<E> eventSource : eventSources) {
      disposables.add(eventSource.subscribe(eventConsumer));
    }

    return new com.spotify.mobius2.disposables.Disposable() {
      @Override
      public void dispose() {
        for (Disposable disposable : disposables) {
          disposable.dispose();
        }
      }
    };
  }
}
