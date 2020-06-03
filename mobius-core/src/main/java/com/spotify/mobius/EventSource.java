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

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/**
 * Interface for event sources.
 *
 * <p>The event source is used for subscribing to events that are external to the Mobius
 * application. This is primarily meant to be used for environmental events - events that come from
 * external signals, like change of network connectivity or a periodic timer, rather than happening
 * because of an effect being triggered or the UI being interacted with.
 *
 * @param <E> the event class
 */
public interface EventSource<E> {

  /**
   * Subscribes the supplied consumer to the events from this event source, until the returned
   * {@link Disposable} is disposed. Multiple such subscriptions can be in place concurrently for a
   * given event source, without affecting each other.
   *
   * @param eventConsumer the consumer that should receive events from the source
   * @return a disposable used to stop the source from emitting any more events to this consumer
   */
  @Nonnull
  Disposable subscribe(Consumer<E> eventConsumer);
}
