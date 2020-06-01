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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spotify.mobius2.runners.ExecutorServiceWorkRunner;
import com.spotify.mobius2.runners.WorkRunner;
import com.spotify.mobius2.test.RecordingModelObserver;
import com.spotify.mobius2.testdomain.TestEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;

public class MobiusLoopErrorReporting extends MobiusLoopTest {

  @Test
  public void shouldIncludeEventInExceptionWhenDispatchFails() throws Exception {
    // given a loop
    observer = new RecordingModelObserver<>();

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    WorkRunner eventRunner = new ExecutorServiceWorkRunner(executorService);

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            eventRunner,
            immediateRunner);

    // whose event workrunner has been disposed.
    eventRunner.dispose();

    // when an event is dispatched,
    // then the exception contains a description of the event.
    assertThatThrownBy(
            () -> mobiusLoop.dispatchEvent(new TestEvent("print me in the exception message")))
        .hasMessageContaining("print me in the exception message");
  }
}
