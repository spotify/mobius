package com.spotify.mobius;

import com.spotify.mobius.functions.Consumer;

/**
 * TODO: document!
 */
public interface EffectRouterBuilder<F, E> {

  <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Runnable action);
  <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Consumer<G> consumer);
  <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Connectable<G, E> connectable);

  Connectable<F, E> build();
}
