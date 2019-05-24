package com.spotify.mobius;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class EventSourceConnectableTest {

  TestEventSource source;
  Connectable<Integer, String> underTest;
  RecordingConsumer<String> events;

  @Before
  public void setUp() throws Exception {
    source = new TestEventSource();
    underTest = EventSourceConnectable.create(source);
    events = new RecordingConsumer<>();
  }

  public static class SubscriptionsBehavior extends EventSourceConnectableTest {
    @Test
    public void subscribesToEventSourceOnFirstModel() {
      final Connection<Integer> connection = underTest.connect(events);
      connection.accept(0);
      assertThat(source.subscriberCount(), is(1));
    }

    @Test
    public void subscribesToEventSourceOnlyOnce() {
      final Connection<Integer> connection = underTest.connect(events);
      connection.accept(0);
      connection.accept(1);
      assertThat(source.subscriberCount(), is(1));
    }

    @Test
    public void disposingUnsubscribesFromEventSource() {
      final Connection<Integer> connection = underTest.connect(events);
      connection.accept(0);
      connection.dispose();
      assertThat(source.subscriberCount(), is(0));
    }

    @Test
    public void disposingBeforeStartingDoesNothing() {
      final Connection<Integer> connection = underTest.connect(events);
      assertThat(source.subscriberCount(), is(0));
      connection.dispose();
      assertThat(source.subscriberCount(), is(0));
    }

    @Test
    public void disposingThenSubscribingResubscribesToEventSource() {
      Connection<Integer> connection = underTest.connect(events);
      connection.accept(0);
      assertThat(source.subscriberCount(), is(1));
      connection.dispose();
      assertThat(source.subscriberCount(), is(0));
      connection = underTest.connect(events);
      connection.accept(1);
      assertThat(source.subscriberCount(), is(1));
    }
  }

  public static class EmissionsBehavior extends EventSourceConnectableTest {
    @Test
    public void forwardsAllEmittedEvents() {
      final Connection<Integer> connection = underTest.connect(events);
      connection.accept(0);
      source.publishEvent("Hello");
      source.publishEvent("World");
      events.assertValues("Hello", "World");
    }

    @Test
    public void noItemsAreEmittedOnceDisposed() {
      final Connection<Integer> connection = underTest.connect(events);
      connection.accept(0);
      source.publishEvent("Hello");
      connection.dispose();
      source.publishEvent("World");
      events.assertValues("Hello");
    }
  }

  private static class TestEventSource implements EventSource<String> {
    private CopyOnWriteArrayList<Consumer<String>> consumers = new CopyOnWriteArrayList<>();

    @Nonnull
    @Override
    public Disposable subscribe(Consumer<String> eventConsumer) {
      consumers.add(eventConsumer);
      return () -> consumers.remove(eventConsumer);
    }

    public void publishEvent(String event) {
      for (Consumer<String> consumer : consumers) {
        consumer.accept(event);
      }
    }

    public int subscriberCount() {
      return consumers.size();
    }
  }
}
