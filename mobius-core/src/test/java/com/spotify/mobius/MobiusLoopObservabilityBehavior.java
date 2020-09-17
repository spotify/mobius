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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.ImmediateWorkRunner;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.testdomain.TestEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusLoopObservabilityBehavior extends MobiusLoopTest {

  @Test
  public void shouldTransitionToNextStateBasedOnInput() throws Exception {
    mobiusLoop.dispatchEvent(new TestEvent("first"));
    mobiusLoop.dispatchEvent(new TestEvent("second"));

    observer.assertStates("init", "init->first", "init->first->second");
  }

  @Test
  public void shouldSupportUnregisteringObserver() throws Exception {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            immediateRunner,
            immediateRunner);

    Disposable unregister = mobiusLoop.observe(observer);

    mobiusLoop.dispatchEvent(new TestEvent("active observer"));
    unregister.dispose();
    mobiusLoop.dispatchEvent(new TestEvent("shouldn't be seen"));

    observer.assertStates("init", "init->active observer");
  }

  @Test
  public void shouldNotReportModelsInIncorrectOrder() throws Exception {
    // 1. create a loop with initial model A
    // 2. concurrently, do:
    //    a. dispatch an event that changes the model to B
    //    b. add an observer
    // 3. verify that the observer never saw B, A.

    ExecutorService service1 = Executors.newSingleThreadExecutor();
    ExecutorService service2 = Executors.newSingleThreadExecutor();

    List<String> bad = new ArrayList<>();

    // 100,000 iterations tended to lead to about 10 instances of 'B,A' before the underlying
    // issue was fixed - but that takes a lot of time for little benefit now that the issue is
    // fixed,  so this iteration count was reduced. If another out-of-order error exists, or the
    // error happens again, this test should become flaky.
    for (int i = 0; i < 1000; i++) {
      MobiusLoop<Integer, Integer, Integer> loop =
          MobiusLoop.create(
              (model, event) -> Next.next(event),
              0,
              emptyList(),
              new NoopConnectable(),
              new NoopConnectable(),
              new ImmediateWorkRunner(),
              new ImmediateWorkRunner());

      List<Integer> observed = Collections.synchronizedList(new ArrayList<>());

      final Future<Disposable> future2 = service2.submit(() -> loop.observe(observed::add));
      final Future<?> future1 = service1.submit(() -> loop.dispatchEvent(1));

      future1.get();
      future2.get();

      assertThat(observed.size()).isBetween(1, 2);

      // two models, with model 0 at the end is bad - adding that to a list so that the test will
      // give a clearer indication of how common this was.
      if (observed.size() == 2 && observed.get(1) == 0) {
        bad.add(observed.toString());
      }
    }

    assertThat(bad).isEmpty();
  }

  private static class NoopConnectable implements Connectable<Integer, Integer> {

    @Nonnull
    @Override
    public Connection<Integer> connect(Consumer<Integer> output)
        throws ConnectionLimitExceededException {
      return new Connection<Integer>() {
        @Override
        public void accept(Integer value) {}

        @Override
        public void dispose() {}
      };
    }
  }
}
