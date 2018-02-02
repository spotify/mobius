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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;

public class RecordingConsumerTest {

  private RecordingConsumer<String> consumer;

  private ExecutorService executorService;

  @Before
  public void setUp() throws Exception {
    consumer = new RecordingConsumer<>();

    executorService = Executors.newSingleThreadExecutor();
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
    CountDownLatch latch = new CountDownLatch(1);

    Future<Boolean> f =
        executorService.submit(
            () -> {
              latch.countDown();
              return consumer.waitForChange(100_000);
            });

    // wait for the other thread to start waiting for a change
    latch.await();

    consumer.accept("heya");

    await().atMost(Duration.FIVE_SECONDS).until(f::isDone);

    assertThat(f.get(), is(true));
  }

  @Test
  public void shouldReturnTrueForNoChange() throws Exception {
    assertThat(consumer.waitForChange(50), is(true));
  }
}
