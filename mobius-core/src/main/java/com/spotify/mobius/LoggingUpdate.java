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

import com.spotify.mobius.internal_util.Throwables;
import javax.annotation.Nonnull;

class LoggingUpdate<M, E, F> implements Update<M, E, F> {

  private final Update<M, E, F> actualUpdate;
  private final MobiusLoop.Logger<M, E, F> logger;

  LoggingUpdate(Update<M, E, F> actualUpdate, MobiusLoop.Logger<M, E, F> logger) {
    this.actualUpdate = checkNotNull(actualUpdate);
    this.logger = checkNotNull(logger);
  }

  @Nonnull
  @Override
  public Next<M, F> update(M model, E event) {
    logger.beforeUpdate(model, event);
    Next<M, F> result = safeInvokeUpdate(model, event);
    logger.afterUpdate(model, event, result);
    return result;
  }

  private Next<M, F> safeInvokeUpdate(M model, E event) {
    try {
      return actualUpdate.update(model, event);
    } catch (Exception e) {
      logger.exceptionDuringUpdate(model, event, e);
      throw Throwables.propagate(e);
    }
  }
}
