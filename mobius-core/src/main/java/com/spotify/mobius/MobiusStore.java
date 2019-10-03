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

import javax.annotation.Nonnull;

/** Responsible for holding and updating the current model. */
class MobiusStore<M, E, F> {

  @Nonnull private final Update<M, E, F> update;

  @Nonnull private M currentModel;

  private MobiusStore(Update<M, E, F> update, M startModel) {
    this.update = checkNotNull(update);
    this.currentModel = checkNotNull(startModel);
  }

  @Nonnull
  public static <M, E, F> MobiusStore<M, E, F> create(Update<M, E, F> update, M startModel) {
    return new MobiusStore<>(update, startModel);
  }

  @Nonnull
  synchronized Next<M, F> update(E event) {
    Next<M, F> next = update.update(currentModel, checkNotNull(event));
    currentModel = next.modelOrElse(currentModel);
    return next;
  }

  M model() {
    return currentModel;
  }
}
