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
import javax.annotation.Nullable;

/** This is the interface for a Mobius loop. */
public interface Loop<M, E, F> extends Disposable {
  /**
   * Dispatch an event to this loop for processing.
   *
   * @param event the event to process
   * @throws IllegalStateException if the loop has been disposed, or if there is an error processing
   *     the event.
   */
  void dispatchEvent(E event);

  @Nullable
  M getMostRecentModel();

  /**
   * Add an observer of model changes to this loop. If {@link #getMostRecentModel()} is non-null,
   * the observer will immediately be notified of the most recent model. The observer will be
   * notified of future changes to the model until the loop or the returned {@link Disposable} is
   * disposed.
   *
   * @param observer a non-null observer of model changes
   * @return a {@link Disposable} that can be used to stop further notifications to the observer
   * @throws NullPointerException if the observer is null
   * @throws IllegalStateException if the loop has been disposed
   */
  Disposable observe(Consumer<M> observer);
}
