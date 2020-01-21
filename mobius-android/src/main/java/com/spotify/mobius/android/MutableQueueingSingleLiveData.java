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
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;

/**
 * An internal implementation of {@link SingleLiveData} that allows posting values. This
 * implementation is backed by an Android Live data and also uses an {@link Accumulator} to allow
 * queueing up values being sent.<br>
 * There are two methods to send values - post and postTransient, the first will queue values, the
 * other will not. See method docs.
 *
 * @param <T> The type of data to store and queue up
 */
final class MutableQueueingSingleLiveData<T> implements SingleLiveData<T> {
  private final MutableLiveData<Accumulator<T>> data = new MutableLiveData<>();

  private static <T> Observer<Accumulator<T>> unfold(Observer<? super T> observer) {
    return accumulator -> {
      if (accumulator != null) {
        accumulator.handle(observer::onChanged);
      }
    };
  }

  @Override
  public boolean hasActiveObservers() {
    return data.hasActiveObservers();
  }

  @Override
  public boolean hasObservers() {
    return data.hasObservers();
  }

  @Override
  public void observe(LifecycleOwner owner, Observer<? super T> observer) {
    data.observe(owner, unfold(observer));
  }

  @Override
  public void observeForever(Observer<? super T> observer) {
    data.observeForever(unfold(observer));
  }

  @Override
  public void removeObservers(LifecycleOwner owner) {
    data.removeObservers(owner);
  }

  /**
   * This method will post the value via the main thread, behaving similarly to MutableLiveData's
   * postValue, with the difference being that if multiple values are being sent while nothing is
   * observing the data, they will be queued up along with any other calls made to post values.
   *
   * @param value The value to emit, or alternatively if nothing is observing, to queue up
   */
  void post(T value) {
    synchronized (data) {
      if (data.getValue() == null) {
        data.postValue(new Accumulator<T>().append(value));
      } else {
        data.postValue(data.getValue().append(value));
      }
    }
  }

  /**
   * This method will post the value via the main thread, behaving similarly to MutableLiveData's
   * postValue.<br>
   * However, if nothing is observing the data at the moment, the value is simply discarded.
   *
   * @param value The value to emit, which will be discarded if nothing is observing it
   */
  void postTransient(T value) {
    if (data.hasActiveObservers()) {
      post(value);
    }
  }
}
