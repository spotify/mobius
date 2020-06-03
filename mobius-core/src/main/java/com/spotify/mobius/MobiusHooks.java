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
package com.spotify.mobius;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows configuration of how Mobius handles programmer errors through setting a custom {@link
 * ErrorHandler} via the {@link #setErrorHandler(ErrorHandler)} method. The default handler logs the
 * error via SLF4J.
 */
public final class MobiusHooks {
  private static final Logger LOGGER = LoggerFactory.getLogger(MobiusHooks.class);

  private static final ErrorHandler DEFAULT_ERROR_HANDLER =
      error -> LOGGER.error("Uncaught error", error);

  @Nonnull private static ErrorHandler errorHandler = DEFAULT_ERROR_HANDLER;

  private MobiusHooks() {
    // prevent instantiation
  }

  public interface ErrorHandler {
    void handleError(Throwable error);
  }

  public static synchronized void handleError(Throwable error) {
    errorHandler.handleError(error);
  }

  /**
   * Changes the error handler that is used by Mobius for internal errors.
   *
   * @param newHandler the new handler to use.
   */
  public static synchronized void setErrorHandler(ErrorHandler newHandler) {
    errorHandler = checkNotNull(newHandler);
  }

  /** Sets the error handler to the default one. */
  public static synchronized void setDefaultErrorHandler() {
    errorHandler = DEFAULT_ERROR_HANDLER;
  }
}
