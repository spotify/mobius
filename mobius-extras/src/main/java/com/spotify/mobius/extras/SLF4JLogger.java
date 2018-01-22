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
package com.spotify.mobius.extras;

import com.spotify.mobius.First;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.Next;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JLogger<M, E, F> implements MobiusLoop.Logger<M, E, F> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SLF4JLogger.class);
  private static final String LOGGING_PREFIX = "Mobius ({}) - ";

  private final String loggingTag;

  public static <M, E, F> MobiusLoop.Logger<M, E, F> withTag(String loggingTag) {
    return new SLF4JLogger<>(loggingTag);
  }

  private SLF4JLogger(String loggingTag) {
    this.loggingTag = loggingTag;
  }

  @Override
  public void beforeInit(M model) {
    LOGGER.debug(LOGGING_PREFIX + "Initializing loop", loggingTag);
  }

  @Override
  public void afterInit(M model, First<M, F> result) {
    LOGGER.debug(
        LOGGING_PREFIX + "Loop initialized, starting from model: {}", loggingTag, result.model());

    for (F effect : result.effects()) {
      LOGGER.debug(LOGGING_PREFIX + "Effect dispatched: {}", loggingTag, effect);
    }
  }

  @Override
  public void exceptionDuringInit(M model, Throwable exception) {
    LOGGER.error("FATAL ERROR: exception during initialization from model {}", model, exception);
  }

  @Override
  public void beforeUpdate(M model, E event) {
    LOGGER.debug(LOGGING_PREFIX + "Event received: {}", loggingTag, event);
  }

  @Override
  public void afterUpdate(M model, E event, Next<M, F> result) {
    if (result.hasModel()) {
      LOGGER.debug(LOGGING_PREFIX + "Model updated: {}", loggingTag, result.modelUnsafe());
    }

    for (F effect : result.effects()) {
      LOGGER.debug(LOGGING_PREFIX + "Effect dispatched: {}", loggingTag, effect);
    }
  }

  @Override
  public void exceptionDuringUpdate(M model, E event, Throwable exception) {
    LOGGER.error(
        "FATAL ERROR: exception updating model '{}' with event '{}'", model, event, exception);
  }
}
