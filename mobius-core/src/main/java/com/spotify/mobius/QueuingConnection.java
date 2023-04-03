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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a connection that queues up messages until a delegate to consume them is available.
 * Useful for setting up circular dependencies safely. Methods are non-blocking.
 */
class QueuingConnection<I> implements Connection<I> {
  private final QueuingDelegate queuingDelegate = new QueuingDelegate();

  /**
   * Tracks the state of this connection; there are three:
   *
   * <p><nl>
   * <li>Initial, when the delegate queues all incoming events. In this state, the delegate is
   *     exactly the {@link #queuingDelegate} instance.
   * <li>Active, when the delegate is the 'real' connection to send events to.
   * <li>Disposed, when the delegate is the {@link #NOOP} instance, which silently discards all
   *     events. </nl>
   */
  private final AtomicReference<Connection<I>> delegate = new AtomicReference<>(queuingDelegate);

  void setDelegate(Connection<I> active) {
    if (delegate.get() == NOOP) {
      return;
    }

    if (!delegate.compareAndSet(queuingDelegate, active)) {
      throw new IllegalStateException("Attempt at setting the active delegate twice");
    }

    // now that we know we have an active connection to send messages to, drain the queue of any
    // messages.
    queuingDelegate.drainQueueIfActive();
  }

  @Override
  public void accept(I value) {
    delegate.get().accept(value);
  }

  @Override
  public void dispose() {
    @SuppressWarnings("unchecked")
    final Connection<I> prev = delegate.getAndSet((Connection<I>) NOOP);

    prev.dispose();
  }

  private static final Connection<?> NOOP =
      new Connection<Object>() {
        @Override
        public void accept(Object value) {}

        @Override
        public void dispose() {}
      };

  private class QueuingDelegate implements Connection<I> {
    private final BlockingQueue<I> queue = new LinkedBlockingQueue<>();

    @Override
    public void accept(I value) {
      queue.add(value);

      // this call solves a race between the setDelegate and accept methods in the outer class:
      // 1. Thread 1 calls accept, and gets the queuing delegate (this class).
      // 2. Thread 2 calls setDelegate, sets the active delegate, and drains the queue.
      // 3. Thread 1 adds an item to the queue via the instruction above.
      //
      // checking if the queue needs draining after each accept solves that.
      drainQueueIfActive();
    }

    @Override
    public void dispose() {
      queue.clear();
    }

    private void drainQueueIfActive() {
      final Connection<I> currentDelegate = delegate.get();

      // if the delegate is no longer the 'queueing delegate', then we're active and should forward
      // the queued messages. We could be 'disposed', but that's fine.
      if (currentDelegate != queuingDelegate) {
        while (true) {
          I value = queue.poll();
          if (value == null) {
            return;
          }
          currentDelegate.accept(value);
        }
      }
    }
  }
}
