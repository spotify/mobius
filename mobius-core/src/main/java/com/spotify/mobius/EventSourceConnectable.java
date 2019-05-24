package com.spotify.mobius;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

class EventSourceConnectable<M, E> implements Connectable<M, E> {

  public static <M, E> Connectable<M, E> create(EventSource<E> source) {
    return new EventSourceConnectable<>(source);
  }

  private final EventSource<E> eventSource;

  private EventSourceConnectable(EventSource<E> source) {
    eventSource = source;
  }

  @Nonnull
  @Override
  public Connection<M> connect(final Consumer<E> output) throws ConnectionLimitExceededException {
    return new Connection<M>() {
      private Disposable disposable;

      @Override
      public void accept(M value) {
        if (disposable != null) {
          return;
        }
        disposable = eventSource.subscribe(output);
      }

      @Override
      public void dispose() {
        if (disposable == null) {
          return;
        }
        disposable.dispose();
      }
    };
  }
}
