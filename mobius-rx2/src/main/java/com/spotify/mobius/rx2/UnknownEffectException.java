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
package com.spotify.mobius.rx2;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

/**
 * Indicates that a {@link MobiusEffectRouter} has received an effect that it hasn't received
 * configuration for. This is a programmer error.
 */
public class UnknownEffectException extends RuntimeException {

  private final Object effect;

  public UnknownEffectException(Object effect) {
    super(checkNotNull(effect).toString());

    this.effect = effect;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnknownEffectException that = (UnknownEffectException) o;

    return effect.equals(that.effect);
  }

  @Override
  public int hashCode() {
    return effect.hashCode();
  }
}
