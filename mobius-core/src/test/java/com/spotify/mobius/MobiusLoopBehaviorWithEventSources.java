package com.spotify.mobius;

import static org.junit.Assert.assertTrue;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.testdomain.TestEvent;
import java.util.concurrent.Semaphore;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusLoopBehaviorWithEventSources extends MobiusLoopTest {
  @Test
  public void invokesEventSourceOnEveryModelUpdate() {
    Semaphore s = new Semaphore(0);
    ConnectableEventSource eventSource = new ConnectableEventSource(s);

    mobiusLoop =
        MobiusLoop.create(
            mobiusStore, effectHandler, eventSource, backgroundRunner, immediateRunner);
    observer = new RecordingModelObserver<>();
    mobiusLoop.observe(observer);
    s.acquireUninterruptibly();
    mobiusLoop.dispose();
    observer.assertStates("init", "init->1", "init->1->2", "init->1->2->3");
    assertTrue(eventSource.disposed);
  }

  @Test
  public void disposesOfEventSourceWhenDisposed() {
    Semaphore s = new Semaphore(0);
    ConnectableEventSource eventSource = new ConnectableEventSource(s);
    mobiusLoop =
        MobiusLoop.create(
            mobiusStore, effectHandler, eventSource, backgroundRunner, immediateRunner);

    mobiusLoop.dispose();
    assertTrue(eventSource.disposed);
  }

  static class ConnectableEventSource implements Connectable<String, TestEvent> {

    private final Semaphore lock;
    boolean disposed;

    ConnectableEventSource(Semaphore lock) {
      this.lock = lock;
    }

    @Nonnull
    @Override
    public Connection<String> connect(Consumer<TestEvent> output)
        throws ConnectionLimitExceededException {
      return new Connection<String>() {

        int count;

        @Override
        public void accept(String value) {
          if (++count > 3) {
            lock.release();
            return;
          }
          output.accept(new TestEvent(Integer.toString(count)));
        }

        @Override
        public void dispose() {
          disposed = true;
        }
      };
    }
  }
}
