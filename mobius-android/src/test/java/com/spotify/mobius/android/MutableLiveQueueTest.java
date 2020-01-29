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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import org.junit.Before;
import org.junit.Test;

public class MutableLiveQueueTest {

  private MutableLiveQueue<String> singleLiveData;

  private FakeLifecycleOwner fakeLifecycleOwner1;
  private FakeLifecycleOwner fakeLifecycleOwner2;
  private RecordingObserver<String> liveObserver;
  private RecordingObserver<Queue<? super String>> pausedObserver;

  @Before
  public void setup() {
    singleLiveData = new MutableLiveQueue<>(WorkRunners.immediate());
    fakeLifecycleOwner1 = new FakeLifecycleOwner();
    fakeLifecycleOwner2 = new FakeLifecycleOwner();
    liveObserver = new RecordingObserver<>();
    pausedObserver = new RecordingObserver<>();
  }

  @Test
  public void shouldIgnoreDestroyedLifecycleOwner() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver);

    assertThat(singleLiveData.hasObserver(), equalTo(false));
  }

  @Test
  public void shouldSendDataToResumedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver);
    singleLiveData.post("one");
    singleLiveData.post("two");

    assertThat(singleLiveData.hasActiveObserver(), equalTo(true));
    liveObserver.assertValues("one", "two");
  }

  @Test
  public void shouldNotQueueEventsWithNoObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    singleLiveData.post("one");
    singleLiveData.post("two");
    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);

    assertThat(liveObserver.valueCount(), equalTo(0));
    assertThat(pausedObserver.valueCount(), equalTo(0));
  }

  @Test
  public void shouldSendQueuedEventsWithValidPausedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    singleLiveData.post("one");
    singleLiveData.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(liveObserver.valueCount(), equalTo(0));
    pausedObserver.assertValues(queueOf("one", "two"));
  }

  @Test
  public void shouldSendLiveAndQueuedEventsWhenRunningAndThenPausedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    singleLiveData.post("one");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    singleLiveData.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    liveObserver.assertValues("one");
    pausedObserver.assertValues(queueOf("two"));
  }

  @Test
  public void shouldSendQueuedEffectsIfObserverSwitchedToResumedOneClearing() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    singleLiveData.post("one");
    singleLiveData.post("two");
    fakeLifecycleOwner2.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    singleLiveData.setObserver(fakeLifecycleOwner2, liveObserver, pausedObserver);

    assertThat(liveObserver.valueCount(), equalTo(0));
    pausedObserver.assertValues(queueOf("one", "two"));
  }

  @Test
  public void shouldSendQueuedEffectsIfObserverSwitchedWithoutClearing() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    singleLiveData.post("one");
    singleLiveData.post("two");
    singleLiveData.setObserver(fakeLifecycleOwner2, liveObserver, pausedObserver);
    fakeLifecycleOwner2.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(liveObserver.valueCount(), equalTo(0));
    pausedObserver.assertValues(queueOf("one", "two"));
  }

  @Test
  public void shouldClearQueueIfObserverCleared() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    singleLiveData.post("one");
    singleLiveData.post("two");
    singleLiveData.clearObserver();
    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(liveObserver.valueCount(), equalTo(0));
    assertThat(pausedObserver.valueCount(), equalTo(0));
  }

  @Test
  public void shouldClearQueueIfLifecycleDestroyed() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    singleLiveData.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    singleLiveData.post("one");
    singleLiveData.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

    assertThat(liveObserver.valueCount(), equalTo(0));
    assertThat(pausedObserver.valueCount(), equalTo(0));
  }

  private Queue<String> queueOf(String... args) {
    return new LinkedList<>(Arrays.asList(args));
  }
}
