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
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.assertj.core.data.Index.atIndex;
import static org.hamcrest.Matchers.containsString;

import com.spotify.mobius.runners.WorkRunners;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

public class MessageDispatcherTest {

  @Test
  public void shouldForwardMessagesToConsumer() throws Exception {
    List<String> messages = new LinkedList<>();

    new MessageDispatcher<String>(WorkRunners.immediate(), messages::add).accept("hey hello");

    assertThat(messages).containsExactly("hey hello");
  }

  @Test
  public void shouldSendErrorsFromConsumerToMobiusHooks() throws Exception {
    // given an error handler
    TestErrorHandler errorHandler = new TestErrorHandler();

    MobiusHooks.setErrorHandler(errorHandler);

    final RuntimeException expected = new RuntimeException("boo");

    // and a message consumer that throws an exception,
    // when a message is dispatched
    new MessageDispatcher<String>(
            WorkRunners.immediate(),
            s -> {
              throw expected;
            })
        .accept("here's an event that should be reported as the cause of failure");

    // then the exception gets sent to the error handler.
    assertThat(errorHandler.handledErrors).extracting(Throwable::getCause).contains(expected);
    assertThat(errorHandler.handledErrors)
        .extracting(Throwable::getMessage)
        .has(
            matching(
                containsString("here's an event that should be reported as the cause of failure")),
            atIndex(0));
  }
}
