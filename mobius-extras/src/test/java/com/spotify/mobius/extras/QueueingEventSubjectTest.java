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
package com.spotify.mobius.extras;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.test.RecordingConsumer;
import org.junit.Before;
import org.junit.Test;

public class QueueingEventSubjectTest {
  private QueueingEventSubject<String> eventSubject;
  private RecordingConsumer<String> receiver;

  @Before
  public void setUp() throws Exception {
    eventSubject = new QueueingEventSubject<>(3);
    receiver = new RecordingConsumer<>();
  }

  @Test
  public void shouldForwardEventsWhenSubscribed() throws Exception {
    eventSubject.subscribe(receiver);

    eventSubject.accept("hey");

    receiver.assertValues("hey");
  }

  @Test
  public void shouldQueueEventsWhenNotSubscribed() throws Exception {
    eventSubject.accept("hi");
    eventSubject.accept("ho");
    eventSubject.accept("to the mines we go");

    eventSubject.subscribe(receiver);

    receiver.assertValues("hi", "ho", "to the mines we go");
  }

  @Test
  public void shouldStopSendingEventsWhenSubscriptionDisposed() throws Exception {
    final Disposable subscription = eventSubject.subscribe(receiver);

    eventSubject.accept("a");
    eventSubject.accept("b");

    subscription.dispose();

    eventSubject.accept("don't want to see this");

    receiver.assertValues("a", "b");
  }

  @Test
  public void shouldOnlySupportASingleSubscriber() throws Exception {
    eventSubject.subscribe(receiver);

    assertThatThrownBy(() -> eventSubject.subscribe(new RecordingConsumer<>()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void shouldSupportUnsubscribeAndResubscribe() throws Exception {
    eventSubject.accept("a");

    Disposable subscription = eventSubject.subscribe(receiver);

    eventSubject.accept("b");

    subscription.dispose();

    eventSubject.accept("c");

    eventSubject.subscribe(receiver);

    receiver.assertValues("a", "b", "c");
  }

  @Test
  public void shouldThrowWhenCapacityExceeded() throws Exception {
    eventSubject.accept("a");
    eventSubject.accept("b");
    eventSubject.accept("c");

    assertThatThrownBy(() -> eventSubject.accept("noo, too many things"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void unsubscribeShouldBeIdempotent() throws Exception {
    // given a subscription that has been disposed
    Disposable subscription1 = eventSubject.subscribe(receiver);

    subscription1.dispose();

    // and a new subscription has been created
    Disposable subscription2 = eventSubject.subscribe(receiver);

    // when the first subscription is disposed again
    subscription1.dispose();

    // then the second subscription is not disposed
    eventSubject.accept("a");

    // and disposing the second subscription works
    subscription2.dispose();

    eventSubject.accept("b");

    receiver.assertValues("a");
  }
}
