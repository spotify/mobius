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

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An interface for an object emitter which emits objects exactly once. This can be used to send
 * effects that need to be handled only once, while also providing a mechanism to queue and handle
 * effects that occur while the lifecycle-owner is in a paused state.<br>
 *
 * @param <T> The type of object to store
 */
public interface LiveQueue<T> {

  /**
   * @return <code>true</code> if the current observer is in a Resumed state<br>
   *     <code>false</code> if the current observer is not Resumed, or there is no current observer
   */
  boolean hasActiveObserver();

  /**
   * @return <code>true</code> if there is an observer of this <code>LiveQueue</code><br>
   *     <code>false</code> if there is no current observer assigned
   */
  boolean hasObserver();

  /**
   * A utility method for calling {@link #setObserver(LifecycleOwner, Observer, Observer)} that
   * substitutes null for the optional observer. See linked method doc for full info.
   */
  void setObserver(
      @Nonnull LifecycleOwner lifecycleOwner, @Nonnull Observer<? super T> liveEffectsObserver);

  /**
   * The <code>LiveQueue</code> supports only a single observer, so calling this method will
   * override any previous observers set.<br>
   * Effects while the lifecycle is active are sent only to the liveEffectsObserver.<br>
   * Once the lifecycle owner goes into Paused state, no effects will be forwarded, however, if the
   * state changes to Resumed, all effects that occurred while Paused will be passed to the optional
   * pausedEffectsObserver. If this optional observer is not provided, these effects will be
   * ignored.<br>
   * Effects that occur while there is no lifecycle owner set will not be queued.
   *
   * @param lifecycleOwner This required parameter is used to queue effects while its state is
   *     Paused and to resume sending effects once it resumes.
   * @param liveEffectsObserver This required observer will be forwarded all effects while the
   *     lifecycle owner is in a Resumed state.
   * @param pausedEffectsObserver The nullable observer will be invoked when the lifecycle owner
   *     resumes, and will receive a queue of effects, ordered as they occurred while paused.
   */
  void setObserver(
      @Nonnull LifecycleOwner lifecycleOwner,
      @Nonnull Observer<? super T> liveEffectsObserver,
      @Nullable Observer<Iterable<? super T>> pausedEffectsObserver);

  /**
   * Removes the current observer and clears any queued effects.<br>
   * To replace the observer without clearing queued effects, use {@link
   * #setObserver(LifecycleOwner, Observer, Observer)}
   */
  void clearObserver();
}
