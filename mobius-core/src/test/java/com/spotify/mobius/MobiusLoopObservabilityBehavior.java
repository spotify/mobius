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
package com.spotify.mobius;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.testdomain.TestEvent;
import org.junit.Test;

public class MobiusLoopObservabilityBehavior extends MobiusLoopTest {
  @Test
  public void shouldTransitionToNextStateBasedOnInput() throws Exception {
    mobiusLoop.dispatchEvent(new TestEvent("first"));
    mobiusLoop.dispatchEvent(new TestEvent("second"));

    observer.assertStates("init", "init->first", "init->first->second");
  }

  @Test
  public void shouldSupportUnregisteringObserver() throws Exception {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            immediateRunner,
            immediateRunner);

    Disposable unregister = mobiusLoop.observe(observer);

    mobiusLoop.dispatchEvent(new TestEvent("active observer"));
    unregister.dispose();
    mobiusLoop.dispatchEvent(new TestEvent("shouldn't be seen"));

    observer.assertStates("init", "init->active observer");
  }
}
