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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class FireAtLeastOnceObserver<V> implements Consumer<V> {

  private final Consumer<V> delegate;
  private volatile boolean hasStartedEmitting = false;
  private final ConcurrentLinkedQueue<V> queue = new ConcurrentLinkedQueue<>();

  public FireAtLeastOnceObserver(Consumer<V> delegate) {
    this.delegate = checkNotNull(delegate);
  }

  @Override
  public void accept(V value) {
    queue.add(value);
    drainQueue();
  }

  private final AtomicReference<AtomicReference<V>> firstValue = new AtomicReference<>(null);

  public void acceptIfFirst(V value) {
    // Wrap the value, so that we are able to represent having a `null` value vs. not having a value
    // at all.
    AtomicReference<V> wrappedValue = new AtomicReference<>(value);
    if (firstValue.compareAndSet(null, wrappedValue)) {
      drainQueue();
    }
  }

  private final AtomicBoolean processing = new AtomicBoolean(false);

  private void drainQueue() {
    if (!processing.compareAndSet(false, true)) {
      // already draining the queue
      return;
    }

    // We are now in a safe section that can only execute on one thread at the time.

    // If this is the first time, try to emit a value that only can be emitted if it is first.
    if (!hasStartedEmitting) {
      hasStartedEmitting = true;
      AtomicReference<V> wrappedValue = firstValue.get();
      if (wrappedValue != null) {
        delegate.accept(wrappedValue.get());
      }
    }

    boolean done = false;

    while (!done) {
      try {
        for (V toSend = queue.poll(); toSend != null; toSend = queue.poll()) {
          delegate.accept(toSend);
        }

      } finally {
        processing.set(false); // leave the safe section

        // If the queue is empty or if we can't reacquire the processing lock, we're done,
        // because either there is nothing to do, or someone else will process the queue.
        // Note: it's important that we check the queue first, otherwise we might leak the lock.
        done = queue.isEmpty() || !processing.compareAndSet(false, true);
      }
    }
  }
}
