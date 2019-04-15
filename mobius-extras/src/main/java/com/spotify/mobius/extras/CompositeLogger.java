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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.First;
import com.spotify.mobius.MobiusLoop.Logger;
import com.spotify.mobius.Next;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link Logger} that delegates all logging to a list of provided loggers. Useful if you have
 * different types of loggers that you would like to us simultaneously while maintaining single
 * responsibility per logger implementation
 *
 * @param <M> The loop's Model type
 * @param <E> The loop's Event type
 * @param <F> The loop's Effect type
 */
public class CompositeLogger<M, E, F> implements Logger<M, E, F> {

  @SafeVarargs
  public static <M, E, F> Logger<M, E, F> from(Logger<M, E, F> logger, Logger<M, E, F>... loggers) {
    List<Logger<M, E, F>> allLoggers = new ArrayList<>();
    allLoggers.add(checkNotNull(logger));
    for (Logger<M, E, F> lg : loggers) {
      allLoggers.add(checkNotNull(lg));
    }
    return new CompositeLogger<>(allLoggers);
  }

  private final List<Logger<M, E, F>> loggers;
  private final List<Logger<M, E, F>> loggersReversed;

  private CompositeLogger(List<Logger<M, E, F>> loggers) {
    this.loggers = loggers;
    this.loggersReversed = new LinkedList<>(loggers);
    Collections.reverse(loggersReversed);
  }

  @Override
  public void beforeInit(M model) {
    for (Logger<M, E, F> logger : loggers) {
      logger.beforeInit(model);
    }
  }

  @Override
  public void afterInit(M model, First<M, F> result) {
    for (Logger<M, E, F> logger : loggersReversed) {
      logger.afterInit(model, result);
    }
  }

  @Override
  public void exceptionDuringInit(M model, Throwable exception) {
    for (Logger<M, E, F> logger : loggersReversed) {
      logger.exceptionDuringInit(model, exception);
    }
  }

  @Override
  public void beforeUpdate(M model, E event) {
    for (Logger<M, E, F> logger : loggers) {
      logger.beforeUpdate(model, event);
    }
  }

  @Override
  public void afterUpdate(M model, E event, Next<M, F> result) {
    for (Logger<M, E, F> logger : loggersReversed) {
      logger.afterUpdate(model, event, result);
    }
  }

  @Override
  public void exceptionDuringUpdate(M model, E event, Throwable exception) {
    for (Logger<M, E, F> logger : loggersReversed) {
      logger.exceptionDuringUpdate(model, event, exception);
    }
  }
}
