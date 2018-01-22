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
package com.spotify.mobius.internal_util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Contains utilities similar to ones found in Guava's Preconditions class. NOT FOR EXTERNAL USE;
 * this class is not a part of the Mobius API and backwards-incompatible changes may happen between
 * releases. If you want to use methods defined here, make your own copy.
 */
public final class Preconditions {
  private Preconditions() {}

  @Nonnull
  public static <T> T checkNotNull(@Nullable T input) {
    if (input == null) {
      throw new NullPointerException();
    }

    return input;
  }

  public static <T> T[] checkArrayNoNulls(T[] input) {
    checkNotNull(input);

    for (T value : input) {
      checkNotNull(value);
    }

    return input;
  }

  public static <I extends Iterable<T>, T> I checkIterableNoNulls(I input) {
    checkNotNull(input);

    for (T value : input) {
      checkNotNull(value);
    }

    return input;
  }
}
