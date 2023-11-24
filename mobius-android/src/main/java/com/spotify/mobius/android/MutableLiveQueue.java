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
package com.spotify.mobius.android;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import com.spotify.mobius.runners.WorkRunner;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An internal implementation of {@link LiveQueue} that allows posting values.
 *
 * @param <T> The type of data to store and queue up
 */
final class MutableLiveQueue<T> implements LiveQueue<T> {

  private class LifecycleObserverHelper implements LifecycleObserver {
    @SuppressWarnings("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    void onAny(LifecycleOwner source, Lifecycle.Event event) {
      onLifecycleChanged(event);
    }
  }

  private final Object lock = new Object();
  private final WorkRunner effectsWorkRunner;
  private final BlockingQueue<T> pausedEffectsQueue;
  @Nullable private Observer<T> liveObserver = null;
  @Nullable private Observer<Iterable<T>> pausedObserver = null;
  private boolean lifecycleOwnerIsPaused = true;
  private boolean ignoreBackgroundEffects = false;

  MutableLiveQueue(WorkRunner effectsWorkRunner, int capacity) {
    this.effectsWorkRunner = effectsWorkRunner;
    this.pausedEffectsQueue = new ArrayBlockingQueue<>(capacity);
  }

  @Override
  public boolean hasActiveObserver() {
    return liveObserver != null && !lifecycleOwnerIsPaused;
  }

  @Override
  public boolean hasObserver() {
    return liveObserver != null;
  }

  @Override
  public void setObserver(@Nonnull LifecycleOwner owner, @Nonnull Observer<T> liveEffectsObserver) {
    setObserver(owner, liveEffectsObserver, null);
  }

  @Override
  public void setObserverIgnoringPausedEffects(
      @Nonnull LifecycleOwner owner, @Nonnull Observer<T> liveEffectsObserver) {
    synchronized (lock) {
      setObserver(owner, liveEffectsObserver, null);
      ignoreBackgroundEffects = true;
    }
  }

  @Override
  public void setObserver(
      @Nonnull LifecycleOwner lifecycleOwner,
      @Nonnull Observer<T> liveObserver,
      @Nullable Observer<Iterable<T>> pausedObserver) {
    if (lifecycleOwner.getLifecycle().getCurrentState() == DESTROYED) {
      return; // ignore
    }
    synchronized (lock) {
      this.liveObserver = liveObserver;
      this.pausedObserver = pausedObserver;
      this.lifecycleOwnerIsPaused = true;
      lifecycleOwner.getLifecycle().addObserver(new LifecycleObserverHelper());
      ignoreBackgroundEffects = false;
    }
  }

  @Override
  public void clearObserver() {
    synchronized (lock) {
      liveObserver = null;
      pausedObserver = null;
      lifecycleOwnerIsPaused = true;
      pausedEffectsQueue.clear();
    }
  }

  /**
   * This method will try to send the posted data to any observers
   *
   * @param data The data to send
   */
  void post(@Nonnull final T data) {
    synchronized (lock) {
      if (lifecycleOwnerIsPaused) {
        if (shouldQueuePausedEffects() && !pausedEffectsQueue.offer(data)) {
          throw new IllegalStateException(
              "Maximum effect queue size ("
                  + pausedEffectsQueue.size()
                  + ") exceeded when posting: "
                  + data);
        }
      } else {
        effectsWorkRunner.post(() -> sendToLiveObserver(data));
      }
    }
  }

  private boolean shouldQueuePausedEffects() {
    return !ignoreBackgroundEffects;
  }

  private void onLifecycleChanged(Lifecycle.Event event) {
    switch (event) {
      case ON_RESUME:
        synchronized (lock) {
          lifecycleOwnerIsPaused = false;
          sendQueuedEffects();
        }
        break;
      case ON_PAUSE:
        synchronized (lock) {
          lifecycleOwnerIsPaused = true;
        }
        break;
      case ON_DESTROY:
        synchronized (lock) {
          clearObserver();
        }
        break;
      default:
        // ignore other events
    }
  }

  // errorprone recommends using ArrayDeque instead of LinkedList here, but ArrayDeque doesn't
  // implement equals, so it's not very useful for testing, and performance isn't going to be an
  // issue here
  @SuppressWarnings("JdkObsolete")
  private void sendQueuedEffects() {
    final Queue<T> queueToSend = new LinkedList<>();
    synchronized (lock) {
      if (lifecycleOwnerIsPaused || pausedObserver == null || pausedEffectsQueue.isEmpty()) {
        return;
      }
      pausedEffectsQueue.drainTo(queueToSend);
    }
    effectsWorkRunner.post(() -> sendToPausedObserver(queueToSend));
  }

  private void sendToLiveObserver(T data) {
    synchronized (lock) {
      if (liveObserver != null) {
        liveObserver.onChanged(data);
      }
    }
  }

  private void sendToPausedObserver(Queue<T> queuedData) {
    synchronized (lock) {
      if (pausedObserver != null) {
        pausedObserver.onChanged(queuedData);
      }
    }
  }
}
