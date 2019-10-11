package com.spotify.mobius.composable;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

/** Implements effect execution for ComposableUpdate-based loops. */
class ComposableUpdateEffectHandler<E> implements Connectable<Effect<E>, E> {

  @Nonnull
  @Override
  public Connection<Effect<E>> connect(Consumer<E> output) {
    return new Connection<Effect<E>>() {
      private Set<Disposable> disposableSet = new HashSet<>();

      @Override
      public void accept(Effect<E> effect) {
        effect.execute(
            new Effect.Callback<E>() {

              private Disposable disposable;

              @Override
              public void onStart(Disposable disposable) {
                this.disposable = disposable;
                disposableSet.add(disposable);
              }

              @Override
              public void onValue(E value) {
                output.accept(value);
              }

              @Override
              public void onComplete() {
                disposableSet.remove(disposable);
              }
            });
      }

      @Override
      public void dispose() {
        for (Disposable disposable : disposableSet) {
          disposable.dispose();
        }
        disposableSet.clear();
      }
    };
  }
}
