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

import static com.spotify.mobius2.Effects.effects;
import static com.spotify.mobius2.internal_util.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.SettableFuture;
import com.spotify.mobius2.disposables.Disposable;
import com.spotify.mobius2.functions.Consumer;
import com.spotify.mobius2.test.RecordingConsumer;
import com.spotify.mobius2.test.RecordingModelObserver;
import com.spotify.mobius2.test.TestWorkRunner;
import com.spotify.mobius2.testdomain.EventWithSafeEffect;
import com.spotify.mobius2.testdomain.SafeEffect;
import com.spotify.mobius2.testdomain.TestEffect;
import com.spotify.mobius2.testdomain.TestEvent;
import java.util.Random;
import java.util.concurrent.ExecutionException;
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
        com.spotify.mobius2.MobiusLoop.create(
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
        com.spotify.mobius2.MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            com.spotify.mobius2.EventSourceConnectable.create(eventSource),
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
        com.spotify.mobius2.MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            com.spotify.mobius2.EventSourceConnectable.create(eventSource),
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

    final com.spotify.mobius2.MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        com.spotify.mobius2.Mobius.loop(
            (model, event) -> {
              updateWasCalled.set(true);
              return com.spotify.mobius2.Next.noChange();
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

    final com.spotify.mobius2.MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        com.spotify.mobius2.Mobius.loop(
            (model, event) -> {
              updateWasCalled.set(true);
              return com.spotify.mobius2.Next.noChange();
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
          return com.spotify.mobius2.Next.next("baz");
        };

    final com.spotify.mobius2.MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        com.spotify.mobius2.Mobius.loop(update, effectHandler);

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

    final com.spotify.mobius2.MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        com.spotify.mobius2.Mobius.loop(
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

  static class EmitDuringDisposeEventSource implements com.spotify.mobius2.EventSource<TestEvent> {

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
    public com.spotify.mobius2.Connection<TestEffect> connect(Consumer<TestEvent> eventConsumer) {
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
