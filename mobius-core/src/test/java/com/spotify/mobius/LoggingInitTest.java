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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class LoggingInitTest {

  private LoggingInit<String, Integer> loggingInit;

  private CapturingLogger<String, Boolean, Integer> logger;

  @Before
  public void setUp() throws Exception {
    Init<String, Integer> delegate =
        new Init<String, Integer>() {
          @Nonnull
          @Override
          public First<String, Integer> init(String model) {
            return First.first(model);
          }
        };
    logger = new CapturingLogger<>();

    loggingInit = new LoggingInit<>(delegate, logger);
  }

  @Test
  public void shouldLogBeforeInit() throws Exception {
    loggingInit.init("tha modell");

    assertThat(logger.beforeInit, contains("tha modell"));
  }

  @Test
  public void shouldLogAfterInit() throws Exception {
    loggingInit.init("tha modell");

    //noinspection unchecked
    assertThat(
        logger.afterInit,
        contains(
            CapturingLogger.AfterInitArgs.create(
                "tha modell", First.<String, Integer>first("tha modell"))));
  }

  @Test
  public void shouldReportExceptions() throws Exception {
    final RuntimeException expected = new RuntimeException("expected");

    loggingInit =
        new LoggingInit<>(
            new Init<String, Integer>() {
              @Nonnull
              @Override
              public First<String, Integer> init(String model) {
                throw expected;
              }
            },
            logger);

    try {
      loggingInit.init("log this plx");
    } catch (Exception e) {
      // ignore
    }

    //noinspection unchecked
    assertThat(
        logger.initErrors,
        contains(CapturingLogger.InitErrorArgs.create("log this plx", expected)));
  }

  @Test
  public void shouldPropagateExceptions() throws Exception {
    final RuntimeException expected = new RuntimeException("expected");
    loggingInit =
        new LoggingInit<>(
            new Init<String, Integer>() {
              @Nonnull
              @Override
              public First<String, Integer> init(String model) {
                throw expected;
              }
            },
            logger);

    assertThatThrownBy(() -> loggingInit.init("hi")).isEqualTo(expected);
  }
}
