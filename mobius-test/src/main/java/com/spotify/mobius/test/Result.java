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
package com.spotify.mobius.test;

import com.google.auto.value.AutoValue;
import com.spotify.mobius.Next;
import javax.annotation.Nonnull;

/** Defines the final state of a Mobius loop after a sequence of events have been processed. */
@AutoValue
public abstract class Result<M, F> {

  /**
   * Returns the final model - note that was not necessarily produced by the last Next, in case that
   * returned an empty model.
   */
  @Nonnull
  public abstract M model();

  /** Returns the Next that resulted from the last processed event */
  @Nonnull
  public abstract Next<M, F> lastNext();

  /**
   * Create a {@link Result} with the provided model and next.
   *
   * @param model the model the loop ended with
   * @param lastNext the last next emitted by the loop
   * @param <M> the model type
   * @param <F> the effect type
   */
  static <M, F> Result<M, F> of(M model, Next<M, F> lastNext) {
    return new AutoValue_Result<>(model, lastNext);
  }
}
