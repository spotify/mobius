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
package com.spotify.mobius.test;

import com.spotify.mobius.functions.Consumer;

public class RecordingModelObserver<S> implements Consumer<S> {

  private final RecordingConsumer<S> recorder = new RecordingConsumer<>();

  @Override
  public void accept(S newModel) {
    recorder.accept(newModel);
  }

  public boolean waitForChange(long timeoutMs) {
    return recorder.waitForChange(timeoutMs);
  }

  public int valueCount() {
    return recorder.valueCount();
  }

  @SafeVarargs
  public final void assertStates(S... expectedStates) {
    recorder.assertValues(expectedStates);
  }
}
