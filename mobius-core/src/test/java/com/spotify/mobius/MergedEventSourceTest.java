package com.spotify.mobius;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MergedEventSourceTest {

  @Test
  public void composesMultipleEventSources() {
    TestEventSource<String> s1 = new TestEventSource<>();
    TestEventSource<String> s2 = new TestEventSource<>();
    TestEventSource<String> s3 = new TestEventSource<>();
    TestEventSource<String> s4 = new TestEventSource<>();

    EventSource<String> mergedSource = MergedEventSource.from(s1, s2, s3, s4);
    RecordingConsumer<String> consumer = new RecordingConsumer<>();
    Disposable disposable = mergedSource.subscribe(consumer);

    s1.send("Hello");
    s3.send("World!");
    s2.send("We");
    s4.send("are");
    s1.send("all");
    s2.send("one");
    s3.send("event");
    s1.send("source");

    consumer.assertValues("Hello", "World!", "We", "are", "all", "one", "event", "source");
    disposable.dispose();
    assertThat(s1.disposed, is(true));
    assertThat(s2.disposed, is(true));
    assertThat(s3.disposed, is(true));
    assertThat(s4.disposed, is(true));
  }

  private static class TestEventSource<T> implements EventSource<T> {

    private Consumer<T> eventConsumer;
    private boolean disposed;

    @Nonnull
    @Override
    public Disposable subscribe(final Consumer<T> eventConsumer) {
      this.eventConsumer = eventConsumer;
      return () -> {
        disposed = true;
        this.eventConsumer = null;
      };
    }

    public void send(T s) {
      if (eventConsumer == null)
        throw new IllegalStateException("Cannot send value without consumer");
      eventConsumer.accept(s);
    }
  }
}
