package com.spotify.mobius.extras;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * An {@link EventSource} that merges multiple sources into one
 *
 * @param <E> The type of Events the sources will emit
 */
public class MergedEventSource<E> implements EventSource<E> {
  private final List<EventSource<E>> eventSources;

  @SafeVarargs
  public static <E> EventSource<E> from(EventSource<E> source, EventSource<E>... sources) {
    List<EventSource<E>> allSources = new ArrayList<>();
    allSources.add(source);
    Collections.addAll(allSources, sources);
    return new MergedEventSource<>(allSources);
  }

  private MergedEventSource(List<EventSource<E>> sources) {
    eventSources = sources;
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<E> eventConsumer) {
    final List<Disposable> disposables = new ArrayList<>(eventSources.size());
    for (EventSource<E> eventSource : eventSources) {
      disposables.add(eventSource.subscribe(eventConsumer));
    }

    return new Disposable() {
      @Override
      public void dispose() {
        for (Disposable disposable : disposables) {
          disposable.dispose();
        }
      }
    };
  }
}