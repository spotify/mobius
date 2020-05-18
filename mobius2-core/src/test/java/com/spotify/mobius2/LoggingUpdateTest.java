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
package com.spotify.mobius2;

import static com.spotify.mobius2.Effects.effects;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class LoggingUpdateTest {

  private com.spotify.mobius2.LoggingUpdate<String, Integer, Boolean> loggingUpdate;

  private CapturingLogger<String, Integer, Boolean> logger;

  @Before
  public void setUp() throws Exception {
    logger = new CapturingLogger<>();

    loggingUpdate =
        new com.spotify.mobius2.LoggingUpdate<>(
            new com.spotify.mobius2.Update<String, Integer, Boolean>() {
              @Nonnull
              @Override
              public com.spotify.mobius2.Next<String, Boolean> update(String model, Integer event) {
                return com.spotify.mobius2.Next.next(model + "-", effects(event % 2 == 0));
              }
            },
            logger);
  }

  @Test
  public void shouldLogBeforeUpdate() throws Exception {
    loggingUpdate.update("mah model", 1);

    assertThat(
        logger.beforeUpdate, contains(CapturingLogger.BeforeUpdateArgs.create("mah model", 1)));
  }

  @Test
  public void shouldLogAfterUpdate() throws Exception {
    loggingUpdate.update("mah model", 1);

    assertThat(
        logger.afterUpdate,
        contains(
            CapturingLogger.AfterUpdateArgs.create(
                "mah model", 1, com.spotify.mobius2.Next.next("mah model-", effects(false)))));
  }

  @Test
  public void shouldReportExceptions() throws Exception {
    final RuntimeException expected = new RuntimeException("expected");

    loggingUpdate =
        new com.spotify.mobius2.LoggingUpdate<>(
            new com.spotify.mobius2.Update<String, Integer, Boolean>() {
              @Nonnull
              @Override
              public com.spotify.mobius2.Next<String, Boolean> update(String model, Integer event) {
                throw expected;
              }
            },
            logger);

    try {
      loggingUpdate.update("log this plx", 13);
    } catch (Exception e) {
      // ignore
    }

    //noinspection unchecked
    assertThat(
        logger.updateErrors,
        contains(CapturingLogger.UpdateErrorArgs.create("log this plx", 13, expected)));
  }

  @Test
  public void shouldPropagateExceptions() throws Exception {
    final RuntimeException expected = new RuntimeException("expected");
    loggingUpdate =
        new com.spotify.mobius2.LoggingUpdate<>(
            new Update<String, Integer, Boolean>() {
              @Nonnull
              @Override
              public Next<String, Boolean> update(String model, Integer event) {
                throw expected;
              }
            },
            logger);

    assertThatThrownBy(() -> loggingUpdate.update("hi", 7)).isEqualTo(expected);
  }
}
