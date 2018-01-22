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

class LoggingInit<M, F> implements Init<M, F> {

  private final Init<M, F> actualInit;
  private final MobiusLoop.Logger<M, ?, F> logger;

  LoggingInit(Init<M, F> actualInit, MobiusLoop.Logger<M, ?, F> logger) {
    this.actualInit = checkNotNull(actualInit);
    this.logger = checkNotNull(logger);
  }

  @Nonnull
  @Override
  public First<M, F> init(M model) {
    logger.beforeInit(model);
    First<M, F> result = safeInvokeInit(model);
    logger.afterInit(model, result);
    return result;
  }

  private First<M, F> safeInvokeInit(M model) {
    try {
      return actualInit.init(model);
    } catch (Exception e) {
      logger.exceptionDuringInit(model, e);
      throw Throwables.propagate(e);
    }
  }
}
