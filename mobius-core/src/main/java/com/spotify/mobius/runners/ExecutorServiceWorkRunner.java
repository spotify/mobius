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
package com.spotify.mobius.runners;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@link WorkRunner} implementation that is backed by an {@link ExecutorService}. */
public class ExecutorServiceWorkRunner implements WorkRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorServiceWorkRunner.class);

  @Nonnull private final ExecutorService service;

  public ExecutorServiceWorkRunner(ExecutorService service) {
    this.service = service;
  }

  @Override
  public void post(Runnable runnable) {
    service.submit(runnable);
  }

  @Override
  public void dispose() {
    try {
      List<Runnable> runnables = service.shutdownNow();

      if (!runnables.isEmpty()) {
        LOGGER.warn(
            "Disposing ExecutorServiceWorkRunner with {} outstanding tasks.", runnables.size());
      }

      if (!service.awaitTermination(100, TimeUnit.MILLISECONDS)) {
        LOGGER.error("ExecutorService shutdown timed out; there are still tasks executing");
      }
    } catch (InterruptedException e) {
      LOGGER.error("Timeout when disposing work runner", e);
    }
  }
}
