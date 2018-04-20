package com.spotify.mobius;

import com.spotify.mobius.functions.Consumer;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * TODO: document!
 * TODO: is this the merged connectable? Or does it have one?
 */
class EffectRouter<F, E> implements Connectable<F, E> {

  @Nonnull
  @Override
  public Connection<F> connect(Consumer<E> output) throws ConnectionLimitExceededException {
    return null;
  }

  static class Builder<F, E> implements EffectRouterBuilder<F, E> {
    private final Map<Class<? extends F>, Connectable<? extends F, E>> connectables;

    Builder() {
      connectables = new HashMap<>();
    }

    @Override
    public <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Runnable action) {
      return add(klazz, Connectables.<G, E>fromRunnable(action));
    }

    @Override
    public <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Consumer<G> consumer) {
      return this;
    }

    @Override
    public <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Connectable<G, E> connectable) {
      connectables.put(klazz, connectable);

      return this;
    }

    @Override
    public Connectable<F, E> build() {
      return new EffectRouter<>();
    }
  }
}
