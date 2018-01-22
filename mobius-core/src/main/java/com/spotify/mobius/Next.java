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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.internal_util.ImmutableUtil;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represents the result of calling an {@link Update} function.
 *
 * <p>Upon calling an Update function with an Event and Model, a Next object will be returned that
 * contains the new Model (if there is one) and Effect objects that describe which side-effects
 * should take place.
 */
@AutoValue
public abstract class Next<M, F> {

  protected Next() {}

  /** Get the model of this Next, if it has one. Might return null. */
  @Nullable
  protected abstract M model();

  /** Check if this Next contains a model. */
  public final boolean hasModel() {
    return model() != null;
  }

  /**
   * Get the effects of this Next.
   *
   * <p>Will return an empty set if there are no effects.
   */
  @Nonnull
  public abstract Set<F> effects();

  /** Check if this Next contains effects. */
  public final boolean hasEffects() {
    return !effects().isEmpty();
  }

  /**
   * Try to get the model from this Next, with a fallback if there isn't one.
   *
   * @param fallbackModel the default model to use if the Next doesn't have a model
   */
  @Nonnull
  public M modelOrElse(M fallbackModel) {
    checkNotNull(fallbackModel);
    if (hasModel()) {
      return modelUnsafe();
    } else {
      return fallbackModel;
    }
  }

  /**
   * Get the model of this Next. This version is unsafe - if this next doesn't have a model, calling
   * this method will cause an exception to be thrown.
   *
   * <p>In almost all cases you should use {@link #modelOrElse} or {@link #ifHasModel} instead.
   *
   * @throws NoSuchElementException if this Next has no model
   */
  @Nonnull
  public M modelUnsafe() {
    if (!hasModel()) {
      throw new NoSuchElementException("there is no model in this Next<>");
    }

    // we know model is never null here since we just checked it.
    //noinspection ConstantConditions
    return model();
  }

  /** If the model is present, call the given consumer with it, otherwise do nothing. */
  public void ifHasModel(Consumer<M> consumer) {
    checkNotNull(consumer);
    if (hasModel()) {
      consumer.accept(modelUnsafe());
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /** Create a Next that updates the model and dispatches the supplied set of effects. */
  @Nonnull
  public static <M, F> Next<M, F> next(M model, Set<? extends F> effects) {
    return new AutoValue_Next<>(model, ImmutableUtil.immutableSet(effects));
  }

  /** Create a Next that updates the model but dispatches no effects. */
  @Nonnull
  public static <M, F> Next<M, F> next(M model) {
    return new AutoValue_Next<>(model, ImmutableUtil.<F>emptySet());
  }

  /** Create a Next that doesn't update the model but dispatches the supplied effects. */
  @Nonnull
  public static <M, F> Next<M, F> dispatch(Set<? extends F> effects) {
    return new AutoValue_Next<>(null, ImmutableUtil.immutableSet(effects));
  }

  /** Create an empty Next that doesn't update the model or dispatch effects. */
  @Nonnull
  public static <M, F> Next<M, F> noChange() {
    return new AutoValue_Next<>(null, ImmutableUtil.<F>emptySet());
  }
}
