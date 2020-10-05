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
package com.spotify.mobius.android;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import androidx.lifecycle.LiveData;
import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/** A class that ties to a live data's active state and can then be used to filter event sources. */
public final class ViewModelEventSourceFilter<M> {
  private final LiveData<M> liveData;

  ViewModelEventSourceFilter(@Nonnull LiveData<M> liveData) {
    this.liveData = checkNotNull(liveData);
  }

  /**
   * Returns a new EventSource that will only emit events from the original event source while the
   * underlying LiveData has any active observers.<br>
   * Events that arrive while there are no observers will be silently discarded.
   *
   * @param eventSource The event source to filter
   * @return A new event source that filters as described.
   */
  @Nonnull
  public <T> EventSource<T> emitWhileModelActive(@Nonnull EventSource<T> eventSource) {
    return new EventSource<T>() {
      @Nonnull
      @Override
      public Disposable subscribe(Consumer<T> eventConsumer) {
        return eventSource.subscribe(
            value -> {
              if (liveData.hasActiveObservers()) {
                eventConsumer.accept(value);
              }
            });
      }
    };
  }
}
