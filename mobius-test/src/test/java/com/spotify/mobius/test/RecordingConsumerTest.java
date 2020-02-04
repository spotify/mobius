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
package com.spotify.mobius.test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.Thread.State;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

public class RecordingConsumerTest {

  private RecordingConsumer<String> consumer;

  @Before
  public void setUp() throws Exception {
    consumer = new RecordingConsumer<>();
  }

  @Test
  public void shouldSupportClearingValues() throws Exception {
    consumer.accept("to be cleared");

    consumer.clearValues();

    consumer.accept("this!");

    consumer.assertValues("this!");
  }

  @Test
  public void shouldTerminateWaitEarlyOnChange() throws Exception {
    AtomicReference<Boolean> waitResult = new AtomicReference<>();

    // given a thread that is blocked waiting for the consumer to get a value
    Thread t =
        new Thread(
            () -> {
              waitResult.set(consumer.waitForChange(100_000));
            });

    t.start();
    await().atMost(Duration.ofSeconds(5)).until(() -> t.getState() == State.TIMED_WAITING);

    // when a value arrives
    consumer.accept("heya");

    // then, in less than 1/10th of the configured waiting time,
    await().atMost(Duration.ofSeconds(10)).until(() -> waitResult.get() != null);

    // the result is 'true'
    assertThat(waitResult.get(), is(true));
  }

  @Test
  public void shouldReturnTrueForNoChange() throws Exception {
    assertThat(consumer.waitForChange(50), is(true));
  }
}
