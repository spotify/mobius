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
package com.spotify.mobius.internal_util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines static utility methods that help working with immutable collections. NOT FOR EXTERNAL
 * USE; this class is not a part of the Mobius API and backwards-incompatible changes may happen
 * between releases. If you want to use methods defined here, make your own copy.
 */
public final class ImmutableUtil {

  private ImmutableUtil() {}

  public static <T> Set<T> emptySet() {
    return Collections.emptySet();
  }

  @SafeVarargs
  public static <T> Set<T> setOf(T... items) {
    Preconditions.checkArrayNoNulls(items);

    Set<T> result = new HashSet<>(items.length);
    Collections.addAll(result, items);

    return Collections.unmodifiableSet(result);
  }

  public static <T> Set<T> immutableSet(Set<? extends T> set) {
    Preconditions.checkIterableNoNulls(set);
    Set<T> result = new HashSet<>(set);
    return Collections.unmodifiableSet(result);
  }

  @SafeVarargs
  public static <T> Set<T> unionSets(Set<? extends T>... sets) {
    Preconditions.checkNotNull(sets);

    Set<T> result = new HashSet<>();
    for (Set<? extends T> set : sets) {
      result.addAll(Preconditions.checkIterableNoNulls(set));
    }

    return Collections.unmodifiableSet(result);
  }
}
