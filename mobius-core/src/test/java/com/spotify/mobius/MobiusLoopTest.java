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

import static com.spotify.mobius.Effects.effects;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.google.common.util.concurrent.SettableFuture;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.ExecutorServiceWorkRunner;
import com.spotify.mobius.runners.ImmediateWorkRunner;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.test.SimpleConnection;
import com.spotify.mobius.test.TestWorkRunner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;

public class MobiusLoopTest {

  private MobiusLoop<String, TestEvent, TestEffect> mobiusLoop;
  private MobiusStore<String, TestEvent, TestEffect> mobiusStore;
  private Connectable<TestEffect, TestEvent> effectHandler;

  private final WorkRunner immediateRunner = new ImmediateWorkRunner();
  private final WorkRunner backgroundRunner =
      new ExecutorServiceWorkRunner(Executors.newSingleThreadExecutor());

  private final EventSource<TestEvent> eventSource =
      new EventSource<TestEvent>() {
        @Nonnull
        @Override
        public Disposable subscribe(Consumer<TestEvent> eventConsumer) {
          return new Disposable() {
            @Override
            public void dispose() {}
          };
        }
      };

  private RecordingModelObserver<String> observer;

  @Before
  public void setUp() throws Exception {
    Init<String, TestEffect> init =
        new Init<String, TestEffect>() {
          @Nonnull
          @Override
          public First<String, TestEffect> init(String model) {
            return First.first(model);
          }
        };

    Update<String, TestEvent, TestEffect> update =
        new Update<String, TestEvent, TestEffect>() {
          @Nonnull
          @Override
          public Next<String, TestEffect> update(String model, TestEvent mobiusEvent) {

            if (mobiusEvent instanceof EventWithCrashingEffect) {
              return Next.next("will crash", effects(new Crash()));
            } else if (mobiusEvent instanceof EventWithSafeEffect) {
              EventWithSafeEffect event = (EventWithSafeEffect) mobiusEvent;
              return Next.next(
                  model + "->" + mobiusEvent.toString(), effects(new SafeEffect(event.toString())));
            } else {
              return Next.next(model + "->" + mobiusEvent.toString());
            }
          }
        };

    mobiusStore = MobiusStore.create(init, update, "init");

    effectHandler =
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                if (effect instanceof Crash) {
                  throw new RuntimeException("Crashing!");
                }
              }
            };

    setupWithEffects(effectHandler, immediateRunner);
  }

  @Test
  public void shouldTransitionToNextStateBasedOnInput() throws Exception {
    mobiusLoop.dispatchEvent(new TestEvent("first"));
    mobiusLoop.dispatchEvent(new TestEvent("second"));

    observer.assertStates("init", "init->first", "init->first->second");
  }

  @Test
  public void shouldSurviveEffectPerformerThrowing() throws Exception {
    mobiusLoop.dispatchEvent(new EventWithCrashingEffect());
    mobiusLoop.dispatchEvent(new TestEvent("should happen"));

    observer.assertStates("init", "will crash", "will crash->should happen");
  }

  @Test
  public void shouldSurviveEffectPerformerThrowingMultipleTimes() throws Exception {
    mobiusLoop.dispatchEvent(new EventWithCrashingEffect());
    mobiusLoop.dispatchEvent(new TestEvent("should happen"));
    mobiusLoop.dispatchEvent(new EventWithCrashingEffect());
    mobiusLoop.dispatchEvent(new TestEvent("should happen, too"));

    observer.assertStates(
        "init",
        "will crash",
        "will crash->should happen",
        "will crash",
        "will crash->should happen, too");
  }

  @Test
  public void shouldSupportEffectsThatGenerateEvents() throws Exception {
    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                eventConsumer.accept(new TestEvent(effect.toString()));
              }
            },
        immediateRunner);

    mobiusLoop.dispatchEvent(new EventWithSafeEffect("hi"));

    observer.assertStates("init", "init->hi", "init->hi->effecthi");
  }

  @Test
  public void shouldOrderStateChangesCorrectlyWhenEffectsAreSlow() throws Exception {
    final SettableFuture<TestEvent> future = SettableFuture.create();

    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                try {
                  eventConsumer.accept(future.get());

                } catch (InterruptedException | ExecutionException e) {
                  e.printStackTrace();
                }
              }
            },
        backgroundRunner);

    mobiusLoop.dispatchEvent(new EventWithSafeEffect("1"));
    mobiusLoop.dispatchEvent(new TestEvent("2"));

    await().atMost(Duration.ONE_SECOND).until(() -> observer.valueCount() >= 3);

    future.set(new TestEvent("3"));

    await().atMost(Duration.ONE_SECOND).until(() -> observer.valueCount() >= 4);
    observer.assertStates("init", "init->1", "init->1->2", "init->1->2->3");
  }

  @Test
  public void shouldSupportHandlingEffectsWhenOneEffectNeverCompletes() throws Exception {
    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                if (effect instanceof SafeEffect) {
                  if (((SafeEffect) effect).id.equals("1")) {
                    try {
                      // Rough approximation of waiting infinite amount of time.
                      Thread.sleep(2000);
                    } catch (InterruptedException e) {
                      // ignored.
                    }
                    return;
                  }
                }

                eventConsumer.accept(new TestEvent(effect.toString()));
              }
            },
        new ExecutorServiceWorkRunner(Executors.newFixedThreadPool(2)));

    // the effectHandler associated with "1" should never happen
    mobiusLoop.dispatchEvent(new EventWithSafeEffect("1"));
    mobiusLoop.dispatchEvent(new TestEvent("2"));
    mobiusLoop.dispatchEvent(new EventWithSafeEffect("3"));

    await().atMost(Duration.FIVE_SECONDS).until(() -> observer.valueCount() >= 5);

    observer.assertStates(
        "init", "init->1", "init->1->2", "init->1->2->3", "init->1->2->3->effect3");
  }

  @Test
  public void shouldPerformEffectFromInit() throws Exception {
    Init<String, TestEffect> init =
        new Init<String, TestEffect>() {
          @Nonnull
          @Override
          public First<String, TestEffect> init(String model) {
            return First.first(model, effects(new SafeEffect("frominit")));
          }
        };

    Update<String, TestEvent, TestEffect> update =
        new Update<String, TestEvent, TestEffect>() {
          @Nonnull
          @Override
          public Next<String, TestEffect> update(String model, TestEvent event) {
            return Next.next(model + "->" + event.toString());
          }
        };

    mobiusStore = MobiusStore.create(init, update, "init");
    TestWorkRunner testWorkRunner = new TestWorkRunner();

    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                eventConsumer.accept(new TestEvent(effect.toString()));
              }
            },
        testWorkRunner);

    observer.waitForChange(100);
    testWorkRunner.runAll();

    observer.assertStates("init", "init->effectfrominit");
  }

  @Test(expected = IllegalStateException.class)
  public void dispatchingEventsAfterDisposalThrowsException() throws Exception {
    mobiusLoop.dispose();
    mobiusLoop.dispatchEvent(new TestEvent("2"));
  }

  @Test
  public void shouldSupportUnregisteringObserver() throws Exception {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(
            mobiusStore, effectHandler, eventSource, immediateRunner, immediateRunner);

    Disposable unregister = mobiusLoop.observe(observer);

    mobiusLoop.dispatchEvent(new TestEvent("active observer"));
    unregister.dispose();
    mobiusLoop.dispatchEvent(new TestEvent("shouldn't be seen"));

    observer.assertStates("init", "init->active observer");
  }

  @Test
  public void shouldThrowForEventSourceEventsAfterDispose() throws Exception {
    FakeEventSource<TestEvent> eventSource = new FakeEventSource<>();

    mobiusLoop =
        MobiusLoop.create(
            mobiusStore, effectHandler, eventSource, immediateRunner, immediateRunner);

    observer = new RecordingModelObserver<>(); // to clear out the init from the previous setup
    mobiusLoop.observe(observer);

    eventSource.emit(new EventWithSafeEffect("one"));
    mobiusLoop.dispose();

    assertThatThrownBy(() -> eventSource.emit(new EventWithSafeEffect("two")))
        .isInstanceOf(IllegalStateException.class);

    observer.assertStates("init", "init->one");
  }

  @Test
  public void shouldThrowForEffectHandlerEventsAfterDispose() throws Exception {
    final FakeEffectHandler effectHandler = new FakeEffectHandler();

    setupWithEffects(effectHandler, immediateRunner);

    effectHandler.emitEvent(new EventWithSafeEffect("good one"));

    mobiusLoop.dispose();

    assertThatThrownBy(() -> effectHandler.emitEvent(new EventWithSafeEffect("bad one")))
        .isInstanceOf(IllegalStateException.class);

    observer.assertStates("init", "init->good one");
  }

  private void setupWithEffects(
      Connectable<TestEffect, TestEvent> effectHandler, WorkRunner effectRunner) {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(mobiusStore, effectHandler, eventSource, immediateRunner, effectRunner);

    mobiusLoop.observe(observer);
  }

  private static class TestEvent {

    private final String name;

    TestEvent(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static class EventWithCrashingEffect extends TestEvent {

    EventWithCrashingEffect() {
      super("crash!");
    }
  }

  private static class EventWithSafeEffect extends TestEvent {

    private EventWithSafeEffect(String id) {
      super(id);
    }
  }

  private interface TestEffect {}

  private static class Crash implements TestEffect {}

  private static class SafeEffect implements TestEffect {

    private final String id;

    private SafeEffect(String id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return "effect" + id;
    }
  }

  private static class FakeEffectHandler implements Connectable<TestEffect, TestEvent> {

    private volatile Consumer<TestEvent> eventConsumer = null;

    void emitEvent(TestEvent event) {
      // throws NPE if not connected; that's OK
      eventConsumer.accept(event);
    }

    @Nonnull
    @Override
    public Connection<TestEffect> connect(Consumer<TestEvent> output)
        throws ConnectionLimitExceededException {
      if (eventConsumer != null) {
        throw new ConnectionLimitExceededException();
      }

      eventConsumer = output;

      return new Connection<TestEffect>() {
        @Override
        public void accept(TestEffect value) {
          // do nothing
        }

        @Override
        public void dispose() {
          // do nothing
        }
      };
    }
  }
}
