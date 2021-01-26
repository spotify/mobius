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

import com.spotify.mobius.EventSource;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/**
 * Interface used by the MobiusLoopViewModel to pass all dependencies necessary to create a
 * MobiusLoop.Factory.
 */
public interface MobiusLoopFactoryProvider<M, E, F, V> {

  /**
   * Creates a MobiusLoop Factory given all the possible dependencies from the MobiusLoopViewModel
   *
   * @param viewEffectConsumer The consumer of View Effects that can be used in your Effect Handler
   * @param activeModelEventSource An Event Source that emits the current active/inactive state of
   *     the ViewModel, based on whether or not the ViewModel's {@link
   *     MobiusLoopViewModel#getModel()} has any active observers or not. This can be used in
   *     conjuncture with <code>ToggledEventSource</code> to be used to control the emissions of any
   *     other given Event Source.
   * @return The factory used to create the loop
   */
  @Nonnull
  MobiusLoop.Factory<M, E, F> create(
      @Nonnull Consumer<V> viewEffectConsumer,
      @Nonnull EventSource<Boolean> activeModelEventSource);
}
