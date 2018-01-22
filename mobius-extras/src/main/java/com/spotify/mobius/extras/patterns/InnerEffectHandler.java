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
package com.spotify.mobius.extras.patterns;

import com.spotify.mobius.Next;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Interface for handling effects from an inner update function when using {@link InnerUpdate}.
 *
 * @param <M> the outer model type
 * @param <F> the outer effect type
 * @param <FI> the inner effect type
 */
public interface InnerEffectHandler<M, F, FI> {
  /**
   * Handle effects emitted from an inner update function.
   *
   * <p>The outer model has already been updated when this method is called, and the arguments let
   * you know if the model was updated or not. When handling effects you may further modify the
   * model, emit new outer effects, or even choose to ignore the updated outer model.
   *
   * @param model the updated outer model
   * @param modelUpdated true if the outer model was updated
   * @param innerEffects the effects emitted by the inner update function
   */
  @Nonnull
  Next<M, F> handleInnerEffects(M model, boolean modelUpdated, Set<FI> innerEffects);
}
