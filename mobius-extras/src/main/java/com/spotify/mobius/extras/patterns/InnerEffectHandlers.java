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

import com.spotify.mobius.Next;
import com.spotify.mobius.functions.Function;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

public class InnerEffectHandlers {

  private InnerEffectHandlers() {}

  /**
   * Create an inner effect handler that ignores inner effects.
   *
   * <p>The resulting next will be an {@link Next#next(Object)} or a {@link Next#noChange()}
   * depending on if the outer model changed.
   */
  public static <M, F, FI> InnerEffectHandler<M, F, FI> ignoreEffects() {
    return new InnerEffectHandler<M, F, FI>() {
      @Nonnull
      @Override
      public Next<M, F> handleInnerEffects(M model, boolean modelUpdated, Set<FI> innerEffects) {
        return modelUpdated ? Next.<M, F>next(model) : Next.<M, F>noChange();
      }
    };
  }

  /**
   * Create an inner effect handler that maps inner effects.
   *
   * <p>This can be used for example to wrap an inner effect in an outer effect, or to map inner
   * effects to outer effects.
   *
   * <p>If there are no inner effects, then the resulting next will be an {@link Next#next(Object)}
   * or a {@link Next#noChange()} depending on if the outer model changed.
   */
  public static <M, F, FI> InnerEffectHandler<M, F, FI> mapEffects(final Function<FI, F> f) {
    return new InnerEffectHandler<M, F, FI>() {
      @Nonnull
      @Override
      public Next<M, F> handleInnerEffects(M model, boolean modelUpdated, Set<FI> innerEffects) {
        if (innerEffects.isEmpty()) {
          return modelUpdated ? Next.<M, F>next(model) : Next.<M, F>noChange();
        }

        Set<F> effects = new HashSet<>();
        for (FI innerEffect : innerEffects) {
          F outerEffect = checkNotNull(f).apply(innerEffect);
          effects.add(checkNotNull(outerEffect));
        }

        if (modelUpdated) {
          return Next.next(model, effects);
        } else {
          return Next.dispatch(effects);
        }
      }
    };
  }
}
