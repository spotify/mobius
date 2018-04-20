package com.spotify.mobius;

import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

/**
 * TODO: document!
 */
public final class Connectables {
  private Connectables() {
    // prevent instantiation
  }

  public static <I, O> Connectable<I, O> fromRunnable(final Runnable action) {
    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            action.run();
          }

          @Override
          public void dispose() {

          }
        };
      }
    };
  }

  public static <I, O> Connectable<I, O> fromConsumer(final Consumer<I> consumer) {
    return new Connectable<I, O>() {
      @Nonnull
      @Override
      public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        return new Connection<I>() {
          @Override
          public void accept(I value) {
            consumer.accept(value);
          }

          @Override
          public void dispose() {

          }
        };
      }
    };
  }
}
