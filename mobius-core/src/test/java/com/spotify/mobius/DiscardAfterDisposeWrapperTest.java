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

import static org.assertj.core.api.Assertions.assertThat;

import com.spotify.mobius.functions.Consumer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class DiscardAfterDisposeWrapperTest {

  @Test
  public void slowConsumerDoesntBlockDispose() throws Exception {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    CountDownLatch waitForAccept = new CountDownLatch(1);
    CountDownLatch waitToAccept = new CountDownLatch(1);

    List<String> accepted = new LinkedList<>();

    Consumer<String> consumer =
        (String s) -> {
          waitForAccept.countDown();
          try {
            // 'await' returns 'false' if it times out. We don't want that
            assertThat(waitToAccept.await(500, TimeUnit.MILLISECONDS)).isTrue();
          } catch (InterruptedException e) {
            throw new RuntimeException("interrupted", e);
          }
          accepted.add(s);
        };

    // Given a wrapper around a consumer whose accept will block
    DiscardAfterDisposeWrapper<String> wrapper = DiscardAfterDisposeWrapper.wrapConsumer(consumer);

    // when the consumer has started accepting
    final Future<?> future = executorService.submit(() -> wrapper.accept("foo"));
    waitForAccept.await();

    // then it is possible to proceed with disposing
    wrapper.dispose();

    // and the consumer will finish without an exception
    waitToAccept.countDown();
    future.get();
    assertThat(accepted).containsExactly("foo");
  }
}
