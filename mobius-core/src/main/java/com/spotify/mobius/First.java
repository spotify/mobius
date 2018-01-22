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
package com.spotify.mobius;

import com.google.auto.value.AutoValue;
import com.spotify.mobius.internal_util.ImmutableUtil;
import java.util.Set;
import javax.annotation.Nonnull;

/** Defines the entry into the initial state of a Mobius loop. */
@AutoValue
public abstract class First<M, F> {

  /** @return the initial model to use */
  @Nonnull
  public abstract M model();

  /** @return the possibly empty set of effects to initially dispatch */
  @Nonnull
  public abstract Set<F> effects();

  /** Check if this First contains effects */
  public final boolean hasEffects() {
    return !effects().isEmpty();
  }

  /**
   * Create a {@link First} with the provided model and no initial effects.
   *
   * @param model the model to initialize the loop with
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> First<M, F> first(M model) {
    return new AutoValue_First<>(model, ImmutableUtil.<F>emptySet());
  }

  /**
   * Create a {@link First} with the provided model and the supplied initial effects.
   *
   * @param model the model to initialize the loop with
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> First<M, F> first(M model, Set<F> effects) {
    return new AutoValue_First<>(model, effects);
  }
}
