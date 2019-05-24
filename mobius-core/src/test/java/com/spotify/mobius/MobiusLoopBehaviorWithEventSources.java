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
