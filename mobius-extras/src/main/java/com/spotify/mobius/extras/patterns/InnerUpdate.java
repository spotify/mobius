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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.spotify.mobius.Next;
import com.spotify.mobius.Update;
import com.spotify.mobius.functions.BiFunction;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/**
 * Helper class for putting an update function inside another update function.
 *
 * <p>It is sometimes useful to compose two update functions that each have their own model, events,
 * and effects. Typically when you do this you will store the inner model inside the outer model,
 * and route some of the outer events to the inner update function. This class helps you wire up
 * this conversion between inner and outer model, events, and effects.
 *
 * <p>The outer update function must still make the decision if the inner update function should be
 * called or not, this class only helps with converting the types of the inner update function
 *
 * @param <M> the outer model type
 * @param <E> the outer event type
 * @param <F> the outer effect type
 * @param <MI> the inner model type
 * @param <EI> the inner event type
 * @param <FI> the inner effect type
 */
@AutoValue
public abstract class InnerUpdate<M, E, F, MI, EI, FI> implements Update<M, E, F> {

  protected abstract Update<MI, EI, FI> innerUpdate();

  protected abstract Function<M, MI> modelExtractor();

  protected abstract Function<E, EI> eventExtractor();

  protected abstract BiFunction<M, MI, M> modelUpdater();

  protected abstract InnerEffectHandler<M, F, FI> innerEffectHandler();

  @Nonnull
  public final Next<M, F> update(M model, E event) {
    MI innerModel = checkNotNull(modelExtractor().apply(model));
    EI innerEvent = checkNotNull(eventExtractor().apply(event));

    Next<MI, FI> innerNext = checkNotNull(innerUpdate().update(innerModel, innerEvent));

    M newModel = model;
    boolean modelUpdated = innerNext.hasModel();

    if (modelUpdated) {
      newModel = checkNotNull(modelUpdater().apply(model, innerNext.modelUnsafe()));
    }

    return checkNotNull(
        innerEffectHandler().handleInnerEffects(newModel, modelUpdated, innerNext.effects()));
  }

  public static <M, E, F, MI, EI, FI> Builder<M, E, F, MI, EI, FI> builder() {
    return new AutoValue_InnerUpdate.Builder<>();
  }

  @AutoValue.Builder
  public abstract static class Builder<M, E, F, MI, EI, FI> {
    /** The inner update function. */
    public abstract Builder<M, E, F, MI, EI, FI> innerUpdate(Update<MI, EI, FI> innerUpdate);

    /** A function that extracts the inner model from an outer model. */
    public abstract Builder<M, E, F, MI, EI, FI> modelExtractor(Function<M, MI> modelExtractor);

    /** A function the extracts the inner event from an outer event. */
    public abstract Builder<M, E, F, MI, EI, FI> eventExtractor(Function<E, EI> eventExtractor);

    /**
     * A bifunction that given the old outer model and the new inner model creates a new outer
     * model.
     */
    public abstract Builder<M, E, F, MI, EI, FI> modelUpdater(BiFunction<M, MI, M> modelUpdater);

    /**
     * An inner effect handler that decides what to do with inner effects. The function is applied
     * after.
     */
    public abstract Builder<M, E, F, MI, EI, FI> innerEffectHandler(
        InnerEffectHandler<M, F, FI> innerEffectHandler);

    public abstract InnerUpdate<M, E, F, MI, EI, FI> build();
  }
}
