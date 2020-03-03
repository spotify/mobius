package com.spotify.mobius.android;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

public class MobiusLoopViewModelTestUtilClasses {
  static class TestEvent {
    final String name;

    TestEvent(String name) {
      this.name = name;
    }
  }

  static class TestEffect {
    final String name;

    TestEffect(String name) {
      this.name = name;
    }
  }

  static class TestModel {
    final String name;

    TestModel(String name) {
      this.name = name;
    }
  }

  static class TestViewEffect {
    final String name;

    TestViewEffect(String name) {
      this.name = name;
    }
  }

  static class TestViewEffectHandler<E, F, V> implements Connectable<F, E> {
    final Consumer<V> viewEffectConsumer;
    private volatile Consumer<E> eventConsumer = null;

    TestViewEffectHandler(Consumer<V> viewEffectConsumer) {
      this.viewEffectConsumer = viewEffectConsumer;
    }

    public void sendEvent(E event) {
      eventConsumer.accept(event);
    }

    @Nonnull
    @Override
    public Connection<F> connect(Consumer<E> output) throws ConnectionLimitExceededException {
      if (eventConsumer != null) {
        throw new ConnectionLimitExceededException();
      }

      eventConsumer = output;

      return new Connection<F>() {
        @Override
        public void accept(F value) {
          // do nothing
        }

        @Override
        public void dispose() {
          // do nothing
        }
      };
    }
  }
}
