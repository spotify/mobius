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
package com.spotify.mobius2.disposables;

import com.spotify.mobius2.internal_util.Preconditions;

/** A {@link Disposable} that disposes of other disposables. */
public class CompositeDisposable implements Disposable {

  private final Disposable[] disposables;

  /**
   * Creates a {@link CompositeDisposable} that holds onto the provided disposables and disposes of
   * all of them once its {@code dispose()} method is called.
   *
   * @param disposables disposables to be disposed of
   * @return a Disposable that mass-disposes of the provided disposables
   */
  public static Disposable from(Disposable... disposables) {
    return new CompositeDisposable(disposables);
  }

  private CompositeDisposable(Disposable[] disposables) {
    this.disposables = new Disposable[disposables.length];
    Preconditions.checkNotNull(disposables);
    System.arraycopy(disposables, 0, this.disposables, 0, disposables.length);
  }

  @Override
  public synchronized void dispose() {
    for (Disposable disposable : disposables) {
      disposable.dispose();
    }
  }
}
