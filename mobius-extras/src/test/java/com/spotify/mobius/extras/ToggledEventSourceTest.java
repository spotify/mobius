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

import static org.assertj.core.api.Assertions.assertThat;

import com.spotify.mobius.disposables.Disposable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ToggledEventSourceTest {

  private FakeDisposingEventSource<String> dataEventSource;
  private ToggledEventSource<String> underTest;
  private QueueingEventSubject<Boolean> togglingEventSource;

  @Before
  public void setUp() {
    dataEventSource = new FakeDisposingEventSource<>();
    togglingEventSource = new QueueingEventSubject<>(10);
    underTest =
        (ToggledEventSource<String>)
            ToggledEventSource.from(dataEventSource, togglingEventSource, false);
  }

  @Test
  public void testThatActiveEventSourceInitializesWithoutSubscribingToBase() {
    togglingEventSource.accept(true);
    dataEventSource.assertConsumerCount(0);
  }

  @Test
  public void testActiveEventSourceWithoutSubscribersResubscribesToBaseWhenSubscriberAdded() {
    togglingEventSource.accept(true);
    underTest.subscribe(value -> {});

    dataEventSource.assertConsumerCount(1);
  }

  @Test
  public void testActiveEventSourceUnsubscribesFromBaseWhenLastSubscriberRemoved() {
    final List<String> received = new ArrayList<>(1);

    togglingEventSource.accept(true);
    underTest.subscribe(received::add).dispose();

    dataEventSource.assertConsumerCount(0);
    assertThat(received).isEmpty();
  }

  @Test
  public void testFilterForwardsEventsWhenActive() {
    final List<String> received = new ArrayList<>(1);
    togglingEventSource.accept(true);
    underTest.subscribe(received::add);

    dataEventSource.emit("a");

    dataEventSource.assertConsumerCount(1);
    assertThat(received).containsExactly("a");
  }

  @Test
  public void testToggledEventSourceBlocksEventsAndRemovesConsumersWhenInactive() {
    final List<String> received = new ArrayList<>(1);
    togglingEventSource.accept(true);
    underTest.subscribe(received::add);
    togglingEventSource.accept(false);
    dataEventSource.emit("b");

    dataEventSource.assertConsumerCount(0);
    assertThat(received).isEmpty();
  }

  @Test
  public void testToggledEventSourceDisposeRemovesFromSourceEventSource() {
    final List<String> received = new ArrayList<>(1);
    togglingEventSource.accept(true);
    Disposable d = underTest.subscribe(received::add);
    d.dispose();
    dataEventSource.emit("c");

    dataEventSource.assertConsumerCount(0);
    assertThat(received).isEmpty();
  }

  @Test
  public void testMultipleSubscribersToToggledAllReceiveForwardedEvents() {
    final List<String> received1 = new ArrayList<>(1);
    final List<String> received2 = new ArrayList<>(1);

    underTest.subscribe(received1::add);
    underTest.subscribe(received2::add);
    togglingEventSource.accept(true);

    dataEventSource.emit("c3");

    assertThat(received1).containsExactly("c3");
    assertThat(received2).containsExactly("c3");
  }

  @Test
  public void testMultipleSubscribersAndOneDisposesThenEventsAreStillForwardedToTheOther() {
    final List<String> received1 = new ArrayList<>(1);
    final List<String> received2 = new ArrayList<>(1);

    underTest.subscribe(received1::add);
    Disposable d2 = underTest.subscribe(received2::add);
    togglingEventSource.accept(true);

    dataEventSource.emit("d41");

    d2.dispose();

    dataEventSource.emit("d42");

    dataEventSource.assertConsumerCount(1); // because our event source is still active
    assertThat(received1).containsExactly("d41", "d42");
    assertThat(received2).containsExactly("d41");
  }

  @Test
  public void testThatToggledEventSourceIsNotKeptAliveByUnderlyingDataSource() {
    final List<String> received = new ArrayList<>(1);

    WeakReference<ToggledEventSource<String>> referenceCheck = new WeakReference<>(underTest);

    togglingEventSource.accept(true);
    Disposable d = underTest.subscribe(received::add);
    dataEventSource.emit("refTest");
    d.dispose();

    underTest = null;
    System.gc();

    assertThat(received).containsExactly("refTest");
    assertThat(underTest).isNull();
    assertThat(referenceCheck.get()).isNull();
  }
}
