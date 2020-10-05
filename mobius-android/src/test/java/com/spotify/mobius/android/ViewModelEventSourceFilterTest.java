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
import androidx.lifecycle.MutableLiveData;
import com.spotify.mobius.EventSource;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestEvent;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestModel;
import com.spotify.mobius.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class ViewModelEventSourceFilterTest {
  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  private final MutableLiveData<TestModel> liveData = new MutableLiveData<>(new TestModel("test"));
  private final FakeLifecycleOwner lifecycleOwner1 = new FakeLifecycleOwner();
  private final FakeLifecycleOwner lifecycleOwner2 = new FakeLifecycleOwner();
  private final EventSourcePublisher<TestEvent> originalEventSource = new EventSourcePublisher<>();

  private ViewModelEventSourceFilter<TestModel> underTest =
      new ViewModelEventSourceFilter<>(liveData);

  @Test
  public void testFilterDoesNotSendEventsWhenLiveDataHasNoObservers() {
    final List<TestEvent> receivedEvents = new ArrayList<>(1);

    EventSource<TestEvent> filtered = underTest.emitWhileModelActive(originalEventSource);

    filtered.subscribe(receivedEvents::add);
    originalEventSource.publish(new TestEvent("test1"));

    assertThat(receivedEvents.size(), equalTo(0));
  }

  @Test
  public void testFilterDoesNotSendEventsWhenLiveDataHasInactiveObservers() {
    final List<TestEvent> receivedEvents = new ArrayList<>(1);

    EventSource<TestEvent> filtered = underTest.emitWhileModelActive(originalEventSource);

    filtered.subscribe(receivedEvents::add);
    liveData.observe(lifecycleOwner1, testModel -> {});
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    originalEventSource.publish(new TestEvent("test2"));

    assertThat(receivedEvents.size(), equalTo(0));
  }

  @Test
  public void testFilterForwardsEventsWhenLiveDataHasAnyActiveObservers() {
    final List<TestEvent> receivedEvents = new ArrayList<>(1);

    EventSource<TestEvent> filtered = underTest.emitWhileModelActive(originalEventSource);

    filtered.subscribe(receivedEvents::add);
    liveData.observe(lifecycleOwner1, testModel -> {});
    liveData.observe(lifecycleOwner2, testModel -> {});
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    originalEventSource.publish(new TestEvent("test3"));

    assertThat(receivedEvents.size(), equalTo(1));
  }

  @Test
  public void testFilteredEventConsumerDisposeCallsOriginalConsumerDispose() {
    final List<TestEvent> receivedEvents = new ArrayList<>(1);

    EventSource<TestEvent> filtered = underTest.emitWhileModelActive(originalEventSource);

    final Disposable disposable = filtered.subscribe(receivedEvents::add);
    liveData.observe(lifecycleOwner1, testModel -> {});
    lifecycleOwner1.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    disposable.dispose();
    originalEventSource.publish(new TestEvent("test4"));

    assertThat(receivedEvents.size(), equalTo(0));
  }
}
