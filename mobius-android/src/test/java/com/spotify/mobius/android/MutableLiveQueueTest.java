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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import androidx.lifecycle.Lifecycle;
import com.spotify.mobius.runners.WorkRunners;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import org.junit.Before;
import org.junit.Test;

public class MutableLiveQueueTest {

  public static final int QUEUE_CAPACITY = 4;
  private MutableLiveQueue<String> mutableLiveQueue;

  private FakeLifecycleOwner fakeLifecycleOwner1;
  private FakeLifecycleOwner fakeLifecycleOwner2;
  private RecordingObserver<String> liveObserver;
  private RecordingObserver<Iterable<String>> pausedObserver;

  @Before
  public void setup() {
    mutableLiveQueue = new MutableLiveQueue<>(WorkRunners.immediate(), QUEUE_CAPACITY);
    fakeLifecycleOwner1 = new FakeLifecycleOwner();
    fakeLifecycleOwner2 = new FakeLifecycleOwner();
    liveObserver = new RecordingObserver<>();
    pausedObserver = new RecordingObserver<>();
  }

  @Test
  public void shouldIgnoreDestroyedLifecycleOwner() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver);

    assertThat(mutableLiveQueue.hasObserver(), equalTo(false));
  }

  @Test
  public void shouldSendDataToResumedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver);
    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");

    assertThat(mutableLiveQueue.hasActiveObserver(), equalTo(true));
    liveObserver.assertValues("one", "two");
  }

  @Test
  public void shouldNotQueueEventsWithNoObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");
    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);

    assertThat(liveObserver.valueCount(), equalTo(0));
    assertThat(pausedObserver.valueCount(), equalTo(0));
  }

  @Test
  public void shouldSendQueuedEventsWithValidPausedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(liveObserver.valueCount(), equalTo(0));
    pausedObserver.assertValues(queueOf("one", "two"));
  }

  @Test
  public void shouldSendLiveAndQueuedEventsWhenRunningAndThenPausedObserver() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    mutableLiveQueue.post("one");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    mutableLiveQueue.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    liveObserver.assertValues("one");
    pausedObserver.assertValues(queueOf("two"));
  }

  @Test
  public void shouldSendQueuedEffectsIfObserverSwappedToResumedOneClearing() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");
    fakeLifecycleOwner2.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    mutableLiveQueue.setObserver(fakeLifecycleOwner2, liveObserver, pausedObserver);

    assertThat(liveObserver.valueCount(), equalTo(0));
    pausedObserver.assertValues(queueOf("one", "two"));
  }

  @Test
  public void shouldSendQueuedEffectsIfObserverSwappedWithoutClearing() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");
    mutableLiveQueue.setObserver(fakeLifecycleOwner2, liveObserver, pausedObserver);
    fakeLifecycleOwner2.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(liveObserver.valueCount(), equalTo(0));
    pausedObserver.assertValues(queueOf("one", "two"));
  }

  @Test
  public void shouldClearQueueIfObserverCleared() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, s -> {}, s -> {});
    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");
    mutableLiveQueue.clearObserver();
    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(liveObserver.valueCount(), equalTo(0));
    assertThat(pausedObserver.valueCount(), equalTo(0));
  }

  @Test
  public void shouldClearQueueIfLifecycleDestroyed() {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver, pausedObserver);
    mutableLiveQueue.post("one");
    mutableLiveQueue.post("two");
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);

    assertThat(liveObserver.valueCount(), equalTo(0));
    assertThat(pausedObserver.valueCount(), equalTo(0));
  }

  @Test
  public void shouldThrowIllegalStateExceptionIfQueueFull() throws Exception {
    fakeLifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

    mutableLiveQueue.setObserver(fakeLifecycleOwner1, liveObserver);

    mutableLiveQueue.post("1");
    mutableLiveQueue.post("2");
    mutableLiveQueue.post("3");
    mutableLiveQueue.post("4");

    assertThatThrownBy(() -> mutableLiveQueue.post("this one breaks"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("this one breaks")
        .hasMessageContaining(String.valueOf(QUEUE_CAPACITY));
  }

  private Queue<String> queueOf(String... args) {
    return new LinkedList<>(Arrays.asList(args));
  }
}
