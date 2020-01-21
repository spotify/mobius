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

import javax.annotation.Nonnull;

/**
 * Represents a View Effect handler that handles effects that specifically require an Android
 * component to handle.<br>
 * This is used by the {@link MobiusLoopViewModel} when creating a loop factory
 *
 * @param <V> The View Effect type
 */
public interface ViewEffectHandler<V> {
  /**
   * Send a view effect to the view, and enqueue it if the view is not currently active, so that it
   * will be handled when/if the view becomes active
   *
   * @param viewEffect The effect to be send to the view
   */
  void post(@Nonnull V viewEffect);

  /**
   * Send a view effect to the to be handled if the view is active, but to be discarded if the view
   * isn't active at this time
   *
   * @param viewEffect The effect to be send to the view
   */
  void postTransient(@Nonnull V viewEffect);
}
