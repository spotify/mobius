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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import android.arch.lifecycle.Lifecycle;
import com.spotify.mobius.runners.WorkRunners;
import org.junit.Before;
import org.junit.Test;

public class MutableQueueingSingleLiveDataTest {

  private MutableQueueingSingleLiveData<String> singleLiveData;

  private FakeLifecycleOwner fakeLifecycleOwner1;
  private FakeLifecycleOwner fakeLifecycleOwner2;
  private RecordingObserver<String> recordingObserver1;
  private RecordingObserver<String> recordingObserver2;

  @Before
  public void setup() {
    singleLiveData = new MutableQueueingSingleLiveData<>(WorkRunners.immediate());
    fakeLifecycleOwner1 = new FakeLifecycleOwner();
    fakeLifecycleOwner2 = new FakeLifecycleOwner();
    recordingObserver1 = new RecordingObserver<>();
    recordingObserver2 = new RecordingObserver<>();
  }

  @Test
  public void shouldIgnoreDestroyedLifecycleOwner() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1);

    assertThat(singleLiveData.hasObserver(), equalTo(false));
  }

  @Test
  public void shouldSendDataToResumedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1);
    singleLiveData.post("one");
    singleLiveData.post("two");

    assertThat(singleLiveData.hasActiveObserver(), equalTo(true));
    recordingObserver1.assertValues("one", "two");
  }

  @Test
  public void shouldNotQueueEventsWithNoObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    singleLiveData.post("one");
    singleLiveData.post("two");
    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1, recordingObserver2);

    assertThat(recordingObserver1.valueCount(), equalTo(0));
    assertThat(recordingObserver2.valueCount(), equalTo(0));
  }

  @Test
  public void shouldSendQueuedEventsWithValidPausedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1, recordingObserver2);
    singleLiveData.post("one");
    singleLiveData.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(recordingObserver1.valueCount(), equalTo(0));
    recordingObserver2.assertValues("one", "two");
  }

  @Test
  public void shouldSendLiveAndQueuedEventsWhenRunningAndThenPausedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1, recordingObserver2);
    singleLiveData.post("one");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    singleLiveData.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    recordingObserver1.assertValues("one");
    recordingObserver2.assertValues("two");
  }

  @Test
  public void shouldSendQueuedEffectsIfObserverSwitchedToResumedOneClearing() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    singleLiveData.post("one");
    singleLiveData.post("two");
    fakeLifecycleOwner2.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    singleLiveData.setObserver(fakeLifecycleOwner2, recordingObserver1, recordingObserver2);

    assertThat(recordingObserver1.valueCount(), equalTo(0));
    recordingObserver2.assertValues("one", "two");
  }

  @Test
  public void shouldSendQueuedEffectsIfObserverSwitchedWithoutClearing() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    singleLiveData.post("one");
    singleLiveData.post("two");
    singleLiveData.setObserver(fakeLifecycleOwner2, recordingObserver1, recordingObserver2);
    fakeLifecycleOwner2.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(recordingObserver1.valueCount(), equalTo(0));
    recordingObserver2.assertValues("one", "two");
  }

  @Test
  public void shouldClearQueueIfObserverCleared() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    singleLiveData.post("one");
    singleLiveData.post("two");
    singleLiveData.clearObserver();
    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1, recordingObserver2);
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(recordingObserver1.valueCount(), equalTo(0));
    assertThat(recordingObserver2.valueCount(), equalTo(0));
  }

  @Test
  public void shouldClearQueueIfLifecycleDestroyed() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, recordingObserver1, recordingObserver2);
    singleLiveData.post("one");
    singleLiveData.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

    assertThat(recordingObserver1.valueCount(), equalTo(0));
    assertThat(recordingObserver2.valueCount(), equalTo(0));
  }
}
