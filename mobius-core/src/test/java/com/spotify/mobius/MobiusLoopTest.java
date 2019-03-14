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
import static com.spotify.mobius.internal_util.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.SettableFuture;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.ExecutorServiceWorkRunner;
import com.spotify.mobius.runners.ImmediateWorkRunner;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.test.RecordingConsumer;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.test.SimpleConnection;
import com.spotify.mobius.test.TestWorkRunner;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;

public class MobiusLoopTest {

  private MobiusLoop<String, TestEvent, TestEffect> mobiusLoop;
  private MobiusStore<String, TestEvent, TestEffect> mobiusStore;
  private Connectable<TestEffect, TestEvent> effectHandler;

  private final WorkRunner immediateRunner = new ImmediateWorkRunner();
  private WorkRunner backgroundRunner;

  private EventSource<TestEvent> eventSource =
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
  private RecordingConsumer<TestEffect> effectObserver;
  private Update<String, TestEvent, TestEffect> update;

  @Before
  public void setUp() throws Exception {
    backgroundRunner = new ExecutorServiceWorkRunner(Executors.newSingleThreadExecutor());
    Init<String, TestEffect> init =
        new Init<String, TestEffect>() {
          @Nonnull
          @Override
          public First<String, TestEffect> init(String model) {
            return First.first(model);
          }
        };

    update =
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
                if (effectObserver != null) {
                  effectObserver.accept(effect);
                }
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
  public void disposingTheLoopDisposesTheWorkRunners() throws Exception {
    TestWorkRunner eventRunner = new TestWorkRunner();
    TestWorkRunner effectRunner = new TestWorkRunner();

    mobiusLoop =
        MobiusLoop.create(mobiusStore, effectHandler, eventSource, eventRunner, effectRunner);

    mobiusLoop.dispose();

    assertTrue("expecting event WorkRunner to be disposed", eventRunner.isDisposed());
    assertTrue("expecting effect WorkRunner to be disposed", effectRunner.isDisposed());
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

  @Test
  public void shouldProcessInitBeforeEventsFromEffectHandler() throws Exception {
    mobiusStore = MobiusStore.create(m -> First.first("I" + m), update, "init");

    // when an effect handler that emits events before returning the connection
    setupWithEffects(
        new Connectable<TestEffect, TestEvent>() {
          @Nonnull
          @Override
          public Connection<TestEffect> connect(Consumer<TestEvent> output)
              throws ConnectionLimitExceededException {
            output.accept(new TestEvent("1"));

            return new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect value) {
                // do nothing
              }
            };
          }
        },
        immediateRunner);

    // in this scenario, the init and the first event get processed before the observer
    // is connected, meaning the 'Iinit' state is never seen
    observer.assertStates("Iinit->1");
  }

  @Test
  public void shouldProcessInitBeforeEventsFromEventSource() throws Exception {
    mobiusStore = MobiusStore.create(m -> First.first("First" + m), update, "init");

    eventSource =
        new EventSource<TestEvent>() {
          @Nonnull
          @Override
          public Disposable subscribe(Consumer<TestEvent> eventConsumer) {
            eventConsumer.accept(new TestEvent("1"));
            return new Disposable() {
              @Override
              public void dispose() {
                // do nothing
              }
            };
          }
        };

    setupWithEffects(new FakeEffectHandler(), immediateRunner);

    // in this scenario, the init and the first event get processed before the observer
    // is connected, meaning the 'Firstinit' state is never seen
    observer.assertStates("Firstinit->1");
  }

  @Test
  public void eventsFromEventSourceDuringDisposeAreIgnored() throws Exception {
    // Events emitted by the event source during dispose should be ignored.

    AtomicBoolean updateWasCalled = new AtomicBoolean();

    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(
            (model, event) -> {
              updateWasCalled.set(true);
              return Next.noChange();
            },
            effectHandler);

    builder
        .eventSource(new EmitDuringDisposeEventSource(new TestEvent("bar")))
        .startFrom("foo")
        .dispose();

    assertFalse(updateWasCalled.get());
  }

  @Test
  public void eventsFromEffectHandlerDuringDisposeAreIgnored() throws Exception {
    // Events emitted by the effect handler during dispose should be ignored.

    AtomicBoolean updateWasCalled = new AtomicBoolean();

    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(
            (model, event) -> {
              updateWasCalled.set(true);
              return Next.noChange();
            },
            new EmitDuringDisposeEffectHandler());

    builder.startFrom("foo").dispose();

    assertFalse(updateWasCalled.get());
  }

  @Test
  public void disposingLoopWhileInitIsRunningDoesNotEmitNewState() throws Exception {
    // Model changes emitted from the init function during dispose should be ignored.

    observer = new RecordingModelObserver<>();
    Semaphore initLock = new Semaphore(0);
    Semaphore awaitDisposalRequest = new Semaphore(0);
    Semaphore disposeLock = new Semaphore(0);
    Semaphore disposalCompleted = new Semaphore(0);

    final Update<String, TestEvent, TestEffect> update = (model, event) -> Next.noChange();
    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(update, effectHandler)
            .init(
                m -> {
                  initLock.acquireUninterruptibly();
                  return First.first(m);
                })
            .eventSource(c -> disposeLock::acquireUninterruptibly);

    mobiusLoop = builder.startFrom("foo");
    mobiusLoop.observe(observer);

    new Thread(
            () -> {
              awaitDisposalRequest.release();
              mobiusLoop.dispose();
              disposalCompleted.release();
            })
        .start();

    awaitDisposalRequest.acquireUninterruptibly();
    initLock.release();
    disposeLock.release();
    disposalCompleted.acquireUninterruptibly();
    observer.assertStates();
  }

  @Test
  public void disposingLoopBeforeInitRunsIgnoresModelFromInit() throws Exception {
    // Model changes emitted from the init function during dispose should be ignored.

    observer = new RecordingModelObserver<>();

    Semaphore awaitInitExecutionRequest = new Semaphore(0);
    Semaphore blockInitExecution = new Semaphore(0);
    Semaphore initExecutionCompleted = new Semaphore(0);

    final Update<String, TestEvent, TestEffect> update = (model, event) -> Next.noChange();
    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(update, effectHandler)
            .eventRunner(
                () ->
                    new WorkRunner() {
                      @Override
                      public void post(Runnable runnable) {
                        backgroundRunner.post(
                            () -> {
                              awaitInitExecutionRequest.release();
                              blockInitExecution.acquireUninterruptibly();
                              runnable.run();
                              initExecutionCompleted.release();
                            });
                      }

                      @Override
                      public void dispose() {
                        backgroundRunner.dispose();
                      }
                    });

    new Thread(() -> mobiusLoop = builder.startFrom("foo")).start();

    awaitInitExecutionRequest.acquireUninterruptibly();

    mobiusLoop.observe(observer);
    mobiusLoop.dispose();

    blockInitExecution.release();
    initExecutionCompleted.acquireUninterruptibly();

    observer.assertStates();
  }

  @Test
  public void modelsFromUpdateDuringDisposeAreIgnored() throws Exception {
    // Model changes emitted from the update function during dispose should be ignored.

    observer = new RecordingModelObserver<>();
    Semaphore lock = new Semaphore(0);

    final Update<String, TestEvent, TestEffect> update =
        (model, event) -> {
          lock.acquireUninterruptibly();
          return Next.next("baz");
        };

    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(update, effectHandler)
            .eventRunner(
                () -> InitImmediatelyThenUpdateConcurrentlyWorkRunner.create(backgroundRunner));

    mobiusLoop = builder.startFrom("foo");
    mobiusLoop.observe(observer);

    mobiusLoop.dispatchEvent(new TestEvent("bar"));
    releaseLockAfterDelay(lock, 30);
    mobiusLoop.dispose();

    observer.assertStates("foo");
  }

  @Test
  public void effectsFromUpdateDuringDisposeAreIgnored() throws Exception {
    // Effects emitted from the update function during dispose should be ignored.

    effectObserver = new RecordingConsumer<>();
    Semaphore lock = new Semaphore(0);

    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(
            (model, event) -> {
              lock.acquireUninterruptibly();
              return Next.dispatch(effects(new SafeEffect("baz")));
            },
            effectHandler);

    mobiusLoop = builder.startFrom("foo");

    mobiusLoop.dispatchEvent(new TestEvent("bar"));
    releaseLockAfterDelay(lock, 30);
    mobiusLoop.dispose();

    effectObserver.assertValues();
  }

  private void setupWithEffects(
      Connectable<TestEffect, TestEvent> effectHandler, WorkRunner effectRunner) {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(mobiusStore, effectHandler, eventSource, immediateRunner, effectRunner);

    mobiusLoop.observe(observer);
  }

  private static void releaseLockAfterDelay(Semaphore lock, int delay) {
    new Thread(
            () -> {
              try {
                Thread.sleep(delay);
              } catch (InterruptedException e) {
                throw propagate(e);
              }

              lock.release();
            })
        .start();
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

  private static class EmitDuringDisposeEventSource implements EventSource<TestEvent> {

    private final TestEvent event;

    public EmitDuringDisposeEventSource(TestEvent event) {
      this.event = event;
    }

    @Nonnull
    @Override
    public Disposable subscribe(Consumer<TestEvent> eventConsumer) {
      return () -> eventConsumer.accept(event);
    }
  }

  private static class EmitDuringDisposeEffectHandler
      implements Connectable<MobiusLoopTest.TestEffect, TestEvent> {

    @Nonnull
    @Override
    public Connection<TestEffect> connect(Consumer<TestEvent> eventConsumer) {
      return new Connection<TestEffect>() {
        @Override
        public void accept(TestEffect value) {
          // ignored
        }

        @Override
        public void dispose() {
          eventConsumer.accept(new TestEvent("bar"));
        }
      };
    }
  }

  @Test
  public void shouldDisposeMultiThreadedEventSourceSafely() throws Exception {
    // event source that just pushes stuff every X ms on a thread.

    RecurringEventSource source = new RecurringEventSource();

    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(update, effectHandler).eventSource(source);

    Random random = new Random();

    for (int i = 0; i < 100; i++) {
      mobiusLoop = builder.startFrom("foo");

      Thread.sleep(random.nextInt(30));

      mobiusLoop.dispose();
    }
  }

  private static class RecurringEventSource implements EventSource<TestEvent> {

    final SettableFuture<Void> completion = SettableFuture.create();

    @Nonnull
    @Override
    public Disposable subscribe(Consumer<TestEvent> eventConsumer) {
      if (completion.isDone()) {
        try {
          completion.get(); // should throw since the only way it can complete is exceptionally
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException("handle this", e);
        }
      }

      final Generator generator = new Generator(eventConsumer);

      Thread t = new Thread(generator);
      t.start();

      return () -> {
        generator.generate = false;
        try {
          t.join();
        } catch (InterruptedException e) {
          throw propagate(e);
        }
      };
    }

    private class Generator implements Runnable {

      private volatile boolean generate = true;
      private final Consumer<TestEvent> consumer;

      private Generator(Consumer<TestEvent> consumer) {
        this.consumer = consumer;
      }

      @Override
      public void run() {
        while (generate) {
          try {
            consumer.accept(new TestEvent("hi"));
            Thread.sleep(15);
          } catch (Exception e) {
            completion.setException(e);
          }
        }
      }
    }
  }

  private static class InitImmediatelyThenUpdateConcurrentlyWorkRunner implements WorkRunner {
    private final WorkRunner delegate;

    private boolean ranOnce;

    private InitImmediatelyThenUpdateConcurrentlyWorkRunner(WorkRunner delegate) {
      this.delegate = delegate;
    }

    public static WorkRunner create(WorkRunner eventRunner) {
      return new InitImmediatelyThenUpdateConcurrentlyWorkRunner(eventRunner);
    }

    @Override
    public synchronized void post(Runnable runnable) {
      if (ranOnce) {
        delegate.post(runnable);
        return;
      }

      ranOnce = true;
      runnable.run();
    }

    @Override
    public void dispose() {
      delegate.dispose();
    }
  }
}
