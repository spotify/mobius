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
package com.spotify.mobius.android;

import static android.arch.lifecycle.Lifecycle.State.DESTROYED;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import com.spotify.mobius.runners.WorkRunner;
import java.util.LinkedList;
import java.util.Queue;
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
  @Nonnull private Queue<T> pausedEffectsQueue = new LinkedList<>();
  @Nullable private Observer<? super T> liveObserver = null;
  @Nullable private Observer<Queue<? super T>> pausedObserver = null;
  private boolean lifecycleOwnerIsPaused = true;

  MutableLiveQueue(WorkRunner effectsWorkRunner) {
    this.effectsWorkRunner = effectsWorkRunner;
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
  public void setObserver(
      @Nonnull LifecycleOwner owner, @Nonnull Observer<? super T> liveEffectsObserver) {
    setObserver(owner, liveEffectsObserver, null);
  }

  @Override
  public void setObserver(
      @Nonnull LifecycleOwner lifecycleOwner,
      @Nonnull Observer<? super T> liveObserver,
      @Nullable Observer<Queue<? super T>> pausedObserver) {
    if (lifecycleOwner.getLifecycle().getCurrentState() == DESTROYED) {
      return; // ignore
    }
    synchronized (lock) {
      this.liveObserver = liveObserver;
      this.pausedObserver = pausedObserver;
      this.lifecycleOwnerIsPaused = true;
      lifecycleOwner.getLifecycle().addObserver(new LifecycleObserverHelper());
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
      if (liveObserver == null) {
        return;
      }
      if (lifecycleOwnerIsPaused) {
        pausedEffectsQueue.offer(data);
      } else {
        effectsWorkRunner.post(() -> liveObserver.onChanged(data));
      }
    }
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
    }
  }

  private void sendQueuedEffects() {
    synchronized (lock) {
      if (lifecycleOwnerIsPaused || pausedObserver == null || pausedEffectsQueue.isEmpty()) {
        return;
      }
      final Queue<T> queueToSend = pausedEffectsQueue;
      pausedEffectsQueue = new LinkedList<>();
      effectsWorkRunner.post(() -> pausedObserver.onChanged(queueToSend));
    }
  }
}
