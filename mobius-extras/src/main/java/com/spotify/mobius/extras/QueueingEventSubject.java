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
package com.spotify.mobius.extras;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Nonnull;

/**
 * An EventSource that can also consume events. If it has a subscriber, events will be immediately
 * forwarded to that subscriber. If it doesn't have a subscriber, it will queue up events (up to the
 * maximum capacity specified in the constructor), and forward all queued events to the next
 * subscriber. Only a single subscription at a time is permitted.
 */
public final class QueueingEventSubject<E> implements EventSource<E>, Consumer<E> {

  private enum State {
    NO_SUBSCRIBER,
    SUBSCRIBED
  }

  private final BlockingQueue<E> queue;

  // these two fields are only read and written within synchronized sections
  private State state;
  private Consumer<E> subscriber;

  public QueueingEventSubject(int capacity) {
    queue = new ArrayBlockingQueue<>(capacity);
    state = State.NO_SUBSCRIBER;
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<E> eventConsumer) {
    List<E> queued = new LinkedList<>();

    // avoid calling the consumer from the synchronized section
    synchronized (this) {
      if (state == State.SUBSCRIBED) {
        throw new IllegalStateException(
            "Only a single subscription is supported, previous subscriber is: " + subscriber);
      }

      state = State.SUBSCRIBED;
      subscriber = eventConsumer;
      queue.drainTo(queued);
    }

    for (E event : queued) {
      eventConsumer.accept(event);
    }

    return new Unsubscriber();
  }

  @Override
  public void accept(E value) {
    Consumer<E> consumerToInvoke = null;

    // avoid calling the consumer from the synchronized section
    synchronized (this) {
      switch (state) {
        case NO_SUBSCRIBER:
          queue.add(value);
          break;
        case SUBSCRIBED:
          consumerToInvoke = subscriber;
          break;
      }
    }

    if (consumerToInvoke != null) {
      consumerToInvoke.accept(value);
    }
  }

  private synchronized void unsubscribe() {
    state = State.NO_SUBSCRIBER;
    subscriber = null;
  }

  private class Unsubscriber implements Disposable {
    private boolean disposed = false;

    @Override
    public synchronized void dispose() {
      if (disposed) {
        return;
      }

      disposed = true;
      unsubscribe();
    }
  }
}
