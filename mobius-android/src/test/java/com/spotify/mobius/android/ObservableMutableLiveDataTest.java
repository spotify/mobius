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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestModel;
import com.spotify.mobius.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class ObservableMutableLiveDataTest {
  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  private final FakeLifecycleOwner lifecycleOwner1 = new FakeLifecycleOwner();

  private final ObservableMutableLiveData<TestModel> underTest = new ObservableMutableLiveData<>();

  @Test
  public void testDataSendsInactiveStateOnSubscribeAndThenActiveStateWhenObserverGoesActive() {
    final List<Boolean> receivedEvents = new ArrayList<>(1);

    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    underTest.observe(lifecycleOwner1, model -> {});
    underTest.subscribe(receivedEvents::add);
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(receivedEvents.size(), equalTo(1));
    assertThat(receivedEvents.get(0), equalTo(true));
  }

  @Test
  public void testDataSendsActiveStateOnSubscribeAndThenInactiveStateWhenObserverGoesInactive() {
    final List<Boolean> receivedEvents = new ArrayList<>(1);

    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    underTest.observe(lifecycleOwner1, model -> {});
    underTest.subscribe(receivedEvents::add);
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_STOP);

    assertThat(receivedEvents.size(), equalTo(1));
    assertThat(receivedEvents.get(0), equalTo(false));
  }

  @Test
  public void testThatDisposeRemovesObserverFromLiveData() {
    final List<Boolean> receivedEvents = new ArrayList<>(1);

    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    underTest.observe(lifecycleOwner1, model -> {});
    underTest.subscribe(receivedEvents::add).dispose();
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_START);

    assertThat(receivedEvents.size(), equalTo(0));
  }

  private Disposable d = null;

  @Test
  public void testThatDisposingFromObserverCallbackDoesNotBreak() {
    final List<Boolean> receivedEvents = new ArrayList<>(1);

    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_START);
    underTest.observe(lifecycleOwner1, model -> {});
    d =
        underTest.subscribe(
            value -> {
              receivedEvents.add(value);
              d.dispose();
            });
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_START);

    // first emitted event recorded is the ON_STOP, which then immediately disposes
    assertThat(receivedEvents.size(), equalTo(1));
    assertThat(receivedEvents.get(0), equalTo(false));
  }
}
