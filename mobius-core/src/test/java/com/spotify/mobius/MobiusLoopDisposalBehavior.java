package com.spotify.mobius;

import static com.spotify.mobius.Effects.effects;
import static com.spotify.mobius.internal_util.Throwables.propagate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.SettableFuture;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.test.RecordingConsumer;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.test.TestWorkRunner;
import com.spotify.mobius.testdomain.EventWithSafeEffect;
import com.spotify.mobius.testdomain.SafeEffect;
import com.spotify.mobius.testdomain.TestEffect;
import com.spotify.mobius.testdomain.TestEvent;
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
        MobiusLoop.create(
            mobiusStore,
            effectHandler,
            EventSourceConnectable.create(eventSource),
            eventRunner,
            effectRunner);

    mobiusLoop.dispose();

    assertTrue("expecting event WorkRunner to be disposed", eventRunner.isDisposed());
    assertTrue("expecting effect WorkRunner to be disposed", effectRunner.isDisposed());
  }

  @Test
  public void shouldThrowForEventSourceEventsAfterDispose() throws Exception {
    FakeEventSource<TestEvent> eventSource = new FakeEventSource<>();

    mobiusLoop =
        MobiusLoop.create(
            mobiusStore,
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
  public void disposingLoopWhileInitIsRunningDoesNotEmitNewState() throws Exception {
    // Model changes emitted from the init function during dispose should be ignored.

    // This test will start a loop and wait until (using the initRequested semaphore) the runnable
    // that runs Init is posted to the event runner. The init function will then be blocked using
    // the initLock semaphore. At this point, we proceed to add the observer then dispose of the
    // loop. The loop is setup with an event source that returns a disposable that will unlock
    // init when it is disposed. So when we dispose of the loop, that will unblock init as part of
    // the disposal procedure. The test then waits until the init runnable has completed running.
    // Completion of the init runnable means:
    // a) init has returned a First
    // b) that first has been unpacked and the model has been set on the store
    // c) that model has been passed back to the loop to be emitted to any state observers
    // Since we're in the process of disposing of the loop, we should see no states in our observer
    observer = new RecordingModelObserver<>();
    Semaphore initLock = new Semaphore(0);
    Semaphore initRequested = new Semaphore(0);
    Semaphore initFinished = new Semaphore(0);

    final Update<String, TestEvent, TestEffect> update = (model, event) -> Next.noChange();
    final MobiusLoop.Builder<String, TestEvent, TestEffect> builder =
        Mobius.loop(update, effectHandler)
            .init(
                m -> {
                  initLock.acquireUninterruptibly();
                  return First.first(m);
                })
            .eventRunner(
                () ->
                    new WorkRunner() {
                      @Override
                      public void post(Runnable runnable) {
                        backgroundRunner.post(
                            () -> {
                              initRequested.release();
                              runnable.run();
                              initFinished.release();
                            });
                      }

                      @Override
                      public void dispose() {
                        backgroundRunner.dispose();
                      }
                    });

    mobiusLoop = builder.startFrom("foo");
    initRequested.acquireUninterruptibly();
    mobiusLoop.observe(observer);
    initLock.release();
    mobiusLoop.dispose();
    initFinished.acquireUninterruptibly(1);
    observer.assertStates();
  }

  @Test
  public void disposingLoopBeforeInitRunsIgnoresModelFromInit() throws Exception {
    // Model changes emitted from the init function after dispose should be ignored.
    // This test sets up the following scenario:
    // 1. The loop is created and initialized on a separate thread
    // 2. The loop is configured with an event runner that will block before executing the init function
    // 3. The test will then dispose of the loop
    // 4. Once the loop is disposed, the test will proceed to unblock the initialization runnable
    // 5. Once the initialization is completed, the test will proceed to examine the observer

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

  static class InitImmediatelyThenUpdateConcurrentlyWorkRunner implements WorkRunner {
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
