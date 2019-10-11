package com.spotify.mobius.composable;

import com.spotify.mobius.Mobius;
import com.spotify.mobius.MobiusLoop;

/**
 * Entry point for the mobius-composable library. Allows you to create mobius loops that use
 * a more composable type of update functions.
 */
public class MobiusComposable {

  /** Create a loop builder from a given ComposableUpdate function. */
  public static <M, E> MobiusLoop.Builder<M, E, Effect<E>> loop(ComposableUpdate<M, E> update) {
    return Mobius.loop(update, new ComposableUpdateEffectHandler<>());
  }
}
