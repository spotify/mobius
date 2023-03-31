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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a connection that queues up messages until a delegate to consume them is available.
 * Useful for setting up circular dependencies safely.
 */
class QueuingConnection<I> implements Connection<I> {

  // The lifecycle has the following stages:
  // - initial, queuing incoming messages
  // - active, forward to delegate
  // - disposed, ignore incoming items.
  //
  // The algorithm runs a risk of forwarding events to the delegate out-of-order in the following
  // situation:
  // 1. The queue has items A and B in it.
  // 2. setDelegate is called, and changes the state to ACTIVE
  // 3. before the queue has been fully drained, accept() is invoked with item C.
  // 4. C may be processed before A and/or B.
  //
  // This is a bit surprising, but in keeping with Mobius's lack of ordering guarantees.

  private enum Lifecycle {
    QUEUING,
    ACTIVE,
    DISPOSED
  }

  private final AtomicReference<Lifecycle> state = new AtomicReference<>(Lifecycle.QUEUING);

  private final BlockingQueue<I> queue = new ArrayBlockingQueue<>(50);
  private final AtomicReference<Connection<I>> delegate = new AtomicReference<>();

  void setDelegate(Connection<I> delegate) {
    if (state.get() == Lifecycle.DISPOSED) {
      return;
    }

    if (!this.delegate.compareAndSet(null, checkNotNull(delegate))) {
      throw new IllegalStateException("Attempt at setting delegate twice");
    }

    if (!state.compareAndSet(Lifecycle.QUEUING, Lifecycle.ACTIVE)) {
      // This set fails if we've been disposed - not going to be common, but let's not forward items
      // to the delegate in that case. And let's make sure that the delegate is disposed,
      // since there's a race between reading the delegate in dispose() and setting it here.
      // Calling dispose twice is safe as per its contract.
      delegate.dispose();
      return;
    }

    // This may fail if we get disposed while we're draining the queue. There's also a race between
    // this draining operation and new items arriving via the accept() method. Both should be
    // acceptable - the first, because the risk is small, and the caller will be in charge of the
    // lifecycle (disposing/setting the delegate), and the latter because of Mobius's lack of
    // ordering guarantees.
    drainQueueIfActive();
  }

  private void drainQueueIfActive() {
    if (state.get() == Lifecycle.ACTIVE) {
      List<I> items = new ArrayList<>();

      queue.drainTo(items);

      for (I item : items) {
        delegate.get().accept(item);
      }
    }
  }

  @Override
  public void accept(I value) {
    switch (state.get()) {
      case QUEUING:
        // here, setDelegate may change state to Active and drain the queue,
        queue.add(value);
        // meaning that we'll need to check if the queue needs to be drained now that we've added
        // an item.
        drainQueueIfActive();
        break;
      case ACTIVE:
        delegate.get().accept(value);
        break;
      case DISPOSED:
        // ignore incoming items when disposed
    }
  }

  @Override
  public void dispose() {
    state.set(Lifecycle.DISPOSED);

    Connection<I> toDispose = delegate.get();

    if (toDispose != null) {
      toDispose.dispose();
    }
  }
}
