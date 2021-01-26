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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import androidx.lifecycle.MutableLiveData;
import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

/** An extension of MutableLiveData that allows its Active/Inactive state to be observed. */
final class ObservableMutableLiveData<T> extends MutableLiveData<T>
    implements EventSource<Boolean> {
  private final List<Consumer<Boolean>> stateListeners = new CopyOnWriteArrayList<>();

  private void notifyListeners(boolean value) {
    for (Consumer<Boolean> listener : stateListeners) {
      listener.accept(value);
    }
  }

  @Override
  protected void onActive() {
    super.onActive();
    notifyListeners(true);
  }

  @Override
  protected void onInactive() {
    super.onInactive();
    notifyListeners(false);
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<Boolean> eventConsumer) {
    stateListeners.add(checkNotNull(eventConsumer));
    return () -> stateListeners.remove(eventConsumer);
  }
}
