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

import static com.spotify.mobius.Effects.effects;
import static com.spotify.mobius.internal_util.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.SettableFuture;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Producer;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.runners.WorkRunners;
import com.spotify.mobius.test.RecordingConsumer;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.test.TestWorkRunner;
import com.spotify.mobius.testdomain.EventWithSafeEffect;
import com.spotify.mobius.testdomain.SafeEffect;
import com.spotify.mobius.testdomain.TestEffect;
import com.spotify.mobius.testdomain.TestEvent;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusLoopDisposalBehavior extends MobiusLoopTest {
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
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            eventRunner,
            effectRunner);

    mobiusLoop.dispose();

    assertTrue("expecting event WorkRunner to be disposed", eventRunner.isDisposed());
    assertTrue("expecting effect WorkRunner to be disposed", effectRunner.isDisposed());
  }

  @Test
  public void shouldIncludeEventTypeAndEventAndModelInErrorMessageForEventsAfterDispose()
      throws Exception {
    FakeEventSource<TestEvent> eventSource = new FakeEventSource<>();

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            EventSourceConnectable.create(eventSource),
            immediateRunner,
            immediateRunner);

    observer = new RecordingModelObserver<>(); // to clear out the init from the previous setup
    mobiusLoop.observe(observer);

    eventSource.emit(new EventWithSafeEffect("one"));
    mobiusLoop.dispose();

    final EventWithSafeEffect event = new EventWithSafeEffect("two");
    assertThatThrownBy(() -> eventSource.emit(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(event.getClass().getName())
        .hasMessageContaining(event.toString())
        .hasMessageContaining(String.valueOf(mobiusLoop.getMostRecentModel()));

    observer.assertStates("init", "init->one");
  }

  @Test
  public void shouldThrowForEventSourceEventsAfterDispose() throws Exception {
    FakeEventSource<TestEvent> eventSource = new FakeEventSource<>();

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            EventSourceConnectable.create(eventSource),
            immediateRunner,
            immediateRunner);

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
        Mobius.loop(update, effectHandler);

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

  @Test
  public void shouldSupportDisposingInObserver() throws Exception {
    RecordingModelObserver<String> secondObserver = new RecordingModelObserver<>();

    // ensure there are some observers to iterate over, and that one of them modifies the
    // observer list.
    // ConcurrentModificationException only triggered if three observers added, for some reason
    Disposable disposable = mobiusLoop.observe(s -> {});
    mobiusLoop.observe(
        s -> {
          if (s.contains("heyho")) {
            disposable.dispose();
          }
        });
    mobiusLoop.observe(s -> {});
    mobiusLoop.observe(secondObserver);

    mobiusLoop.dispatchEvent(new TestEvent("heyho"));

    secondObserver.assertStates("init", "init->heyho");
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

  @Test
  public void shouldSafelyDisposeWhenDisposeAndEventsAreOnDifferentThreads() throws Exception {
    final Random random = new Random();
    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(update, effectHandler)
            .eventRunner(
                new Producer<>() {
                  @Nonnull
                  @Override
                  public WorkRunner get() {
                    return WorkRunners.from(Executors.newFixedThreadPool(4));
                  }
                });
    final Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                for (int i = 0; i < 100; i++) {
                  mobiusLoop = builder.startFrom("foo");
                  try {
                    Thread.sleep(random.nextInt(10));
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                  mobiusLoop.dispose();
                }
              }
            });
    thread.start();

    for (int i = 0; i < 1000; i++) {
      try {
        mobiusLoop.dispatchEvent(new TestEvent("bar"));
        Thread.sleep(1);
      } catch (IllegalStateException e) {
        if (e.getMessage() != null) {
          assertFalse(e.getMessage().startsWith("Exception processing event"));
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    thread.join();
  }

  static void releaseLockAfterDelay(Semaphore lock, int delay) {
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

  static class EmitDuringDisposeEventSource implements EventSource<TestEvent> {

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

  static class EmitDuringDisposeEffectHandler implements Connectable<TestEffect, TestEvent> {

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

  static class RecurringEventSource implements EventSource<TestEvent> {

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
}
