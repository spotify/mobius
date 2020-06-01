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

import com.spotify.mobius2.internal_util.Preconditions;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/** Utility class for working with effects. */
public final class Effects {

  private Effects() {
    // prevent instantiation
  }

  /**
   * Convenience method for instantiating a set of effects. Note that this returns a mutable set of
   * effects to avoid creating too many copies - the set will normally be added to a {@link Next} or
   * {@link First}, leading to another safe-copy being made.
   *
   * @return a *mutable* set of effects
   */
  @SafeVarargs
  @Nonnull
  // implementation note: the type signature of this method helps ensure that you can get a set of a
  // super type even if you only submit items of a sub type. Hence the 'G extends F' type parameter.
  public static <F, G extends F> Set<F> effects(G... effects) {
    Set<F> result = new HashSet<>(effects.length);
    Collections.addAll(result, (F[]) Preconditions.checkArrayNoNulls((F[]) effects));

    return result;
  }
}
