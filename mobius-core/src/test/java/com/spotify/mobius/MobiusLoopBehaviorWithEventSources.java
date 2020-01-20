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
package com.spotify.mobius;

import static com.spotify.mobius.Next.next;
import static com.spotify.mobius.Next.noChange;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.WorkRunners;
import com.spotify.mobius.test.RecordingConsumer;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.testdomain.EventWithSafeEffect;
import com.spotify.mobius.testdomain.TestEffect;
import com.spotify.mobius.testdomain.TestEvent;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusLoopBehaviorWithEventSources extends MobiusLoopTest {

  @Test
  public void invokesEventSourceOnlyOnModelUpdates() {
    ModelRecordingConnectableEventSource eventSource = new ModelRecordingConnectableEventSource();

    update =
        (s, e) -> {
          if (e instanceof EventWithSafeEffect) {
            return next(s + "->" + e.toString());
          } else {
            return noChange();
          }
        };

    final MobiusLoop<String, TestEvent, TestEffect> loop =
        Mobius.loop(update, new FakeEffectHandler())
            .eventRunner(WorkRunners::immediate)
            .effectRunner(WorkRunners::immediate)
            .eventSource(eventSource)
            .startFrom("init");

    loop.dispatchEvent(new TestEvent("This"));
    loop.dispatchEvent(new EventWithSafeEffect("1"));
    loop.dispatchEvent(new TestEvent("will not"));
    loop.dispatchEvent(new EventWithSafeEffect("2"));
    loop.dispatchEvent(new TestEvent("change"));
    loop.dispatchEvent(new TestEvent("state"));

    eventSource.receivedModels.assertValues("init", "init->1", "init->1->2");
    assertThat(eventSource.receivedModels.valueCount(), is(3));
  }

  @Test
  public void processesEventsFromEventSources() {
    ModelRecordingConnectableEventSource eventSource = new ModelRecordingConnectableEventSource();

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            immediateRunner,
            immediateRunner);
    observer = new RecordingModelObserver<>();
    mobiusLoop.observe(observer);
    eventSource.consumer.accept(new TestEvent(Integer.toString(1)));
    eventSource.consumer.accept(new TestEvent(Integer.toString(2)));
    eventSource.consumer.accept(new TestEvent(Integer.toString(3)));
    observer.assertStates("init", "init->1", "init->1->2", "init->1->2->3");
  }

  @Test
  public void disposesOfEventSourceWhenDisposed() {
    ModelRecordingConnectableEventSource eventSource = new ModelRecordingConnectableEventSource();
    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            immediateRunner,
            immediateRunner);

    mobiusLoop.dispose();
    assertTrue(eventSource.disposed);
  }

  @Test
  public void shouldSupportEventSourcesThatEmitOnConnect() throws Exception {
    // given an event source that immediately emits an event (id 1) on connect
    ImmediateEmitter eventSource = new ImmediateEmitter();

    // when we create a mobius loop
    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            immediateRunner,
            immediateRunner);

    // then the event source should receive the initial model as well as the one following from
    // its emitted event
    eventSource.receivedModels.assertValues("init", "init->1");
  }

  static class ModelRecordingConnectableEventSource implements Connectable<String, TestEvent> {

    final RecordingConsumer<String> receivedModels = new RecordingConsumer<>();
    boolean disposed;
    private Consumer<TestEvent> consumer;

    @Nonnull
    @Override
    public Connection<String> connect(Consumer<TestEvent> output) {
      consumer = output;
      return new Connection<String>() {
        @Override
        public void accept(String value) {
          receivedModels.accept(value);
        }

        @Override
        public void dispose() {
          disposed = true;
        }
      };
    }
  }

  static class ImmediateEmitter implements Connectable<String, TestEvent> {
    final RecordingConsumer<String> receivedModels = new RecordingConsumer<>();

    @Nonnull
    @Override
    public Connection<String> connect(Consumer<TestEvent> output)
        throws ConnectionLimitExceededException {
      output.accept(new EventWithSafeEffect("1"));
      return new Connection<String>() {
        @Override
        public void accept(String value) {
          receivedModels.accept(value);
        }

        @Override
        public void dispose() {}
      };
    }
  }
}
