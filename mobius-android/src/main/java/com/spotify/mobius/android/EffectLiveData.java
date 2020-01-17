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

/**
 * An interface for an object that acts like an Android LiveData, except it will only emit each
 * object it receives once and only once.<br>
 * This is meant to be used as a means of sending single-handle Events to the view
 *
 * @param <T> The type of object to store
 */
public interface EffectLiveData<T> {

  boolean hasActiveObservers();

  boolean hasObservers();

  void observe(LifecycleOwner owner, Observer<? super T> observer);

  void observeForever(Observer<? super T> observer);

  void removeObservers(LifecycleOwner owner);
}
