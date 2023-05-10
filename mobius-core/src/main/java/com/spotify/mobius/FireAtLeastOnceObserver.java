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

import com.spotify.mobius.functions.Consumer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FireAtLeastOnceObserver<V> implements Consumer<V> {
  private enum State {
    UNFIRED,
    FIRING,
    READY,
  }

  private final Consumer<V> delegate;
  private volatile State state = State.UNFIRED;
  private final ConcurrentLinkedQueue<V> queue = new ConcurrentLinkedQueue<>();

  public FireAtLeastOnceObserver(Consumer<V> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public void accept(V value) {
    // this is a bit racy, with three threads handling values with order 1, 2 and 3, respectively:
    // 1. thread 1 has called accceptIfUnfired and is in safeConsume, having published its value to
    //    observers, and having just set the state to READY
    // 2. thread 2 has called accept, and is in safeConsume, before the first synchronized section
    // 3. thread 3 has called accept and is about to check the current state.
    //
    // now, if thread 3 reads READY and calls the delegate's accept method directly, before
    // thread 2 sets the state to FIRING and publishes its data, the observer will see 1, 3, 2.
    // this means that this class isn't safe for racing calls to accept(), but given that it's
    // only intended to be used within the event processing, which is sequential, that is not a
    // risk.
    // do note that this class isn't generally useful outside the specific context of event
    // processing.
    if (state != State.READY) {
      safeConsume(value, true);
    } else {
      delegate.accept(value);
    }
  }

  public void acceptIfFirst(V value) {
    if (state == State.UNFIRED) {
      safeConsume(value, false);
    }
  }

  private void safeConsume(V value, boolean acceptAlways) {
    // this synchronized section mustn't call unsafe external code like the delegate's accept
    // method to avoid the risk of deadlocks. It's synchronized because it's changing two stateful
    // fields: the 'state' and the 'queue', and those need to go together to guarantee ordering
    // of the emitted values.
    synchronized (this) {
      // add this item to the queue if we haven't fired, or if it should be added anyway
      if (state == State.UNFIRED || acceptAlways) {
        queue.add(value);
      }

      // set state to FIRING to prevent acceptIfUnfired from adding items to the queue and messing
      // ordering up - regular accept mustn't invoke the delegate consumer directly until we've
      // processed the queue and entered READY state.
      state = State.FIRING;
    }

    for (V toSend = queue.poll(); toSend != null; toSend = queue.poll()) {
      delegate.accept(value);
    }

    synchronized (this) {
      // it's possible for a racing 'accept' call to add an item to the queue after the last poll
      // above, so check in an exclusive way that the queue is in fact empty TODO: not correct.
      if (queue.isEmpty()) {
        state = State.READY;
      }
    }
  }
}
