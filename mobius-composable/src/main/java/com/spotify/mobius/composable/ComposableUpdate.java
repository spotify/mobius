package com.spotify.mobius.composable;

import static com.spotify.mobius.Next.dispatch;
import static com.spotify.mobius.Next.next;
import static com.spotify.mobius.Next.noChange;

import com.spotify.mobius.Next;
import com.spotify.mobius.Update;
import com.spotify.mobius.internal_util.Preconditions;
import java.util.HashSet;
import java.util.Set;

/**
 * A mobius Update function with a fixed effect type, to allow for some more powerful composition
 * patterns.
 */
public interface ComposableUpdate<M, E> extends Update<M, E, Effect<E>> {

  /**
   * Merge a number of update functions into a single update function.
   *
   * <p>The update functions will all be called in order, and any model changes will propagate from
   * one to the next. All effects will be collected from the updates too, and added to the final
   * Next object.
   */
  @SafeVarargs
  static <M, E> ComposableUpdate<M, E> merge(
      ComposableUpdate<M, E> first, ComposableUpdate<M, E>... updates) {
    Preconditions.checkNotNull(first);
    Preconditions.checkArrayNoNulls(updates);

    if (updates.length == 0) return first;

    return (model, event) -> {
      Next<M, Effect<E>> next = first.update(model, event);

      M bestModel = next.modelOrElse(model);
      boolean didAnyoneChange = next.hasModel();
      Set<Effect<E>> allEffects = new HashSet<>(next.effects());

      for (ComposableUpdate<M, E> update : updates) {
        next = update.update(bestModel, event);

        bestModel = next.modelOrElse(bestModel);
        didAnyoneChange |= next.hasModel();
        allEffects.addAll(next.effects());
      }

      if (didAnyoneChange) {
        return next(bestModel, allEffects);
      } else {
        return dispatch(allEffects);
      }
    };
  }

  /**
   * "Pull back" the model and event type of an inner update into the domain of an outer update.
   *
   * <p>The model lens will be used to extract the inner model from the outer model, and to put back
   * any changes done to the inner model.
   *
   * <p>The event prism will be used to filter out and unwrap events that are meant for the inner
   * update, and will wrap any events emitted from the effects of the inner update so that they are
   * routed back to the inner update.
   */
  static <M, E, X, Y> ComposableUpdate<X, Y> pullback(
      Lens<X, M> modelLens, Prism<Y, E> eventPrism, ComposableUpdate<M, E> inner) {
    return (model, event) -> {
      E innerEvent = eventPrism.extract(event);
      if (innerEvent == null) {
        return noChange();
      }

      M innerModel = modelLens.get(model);

      return inner
          .update(innerModel, innerEvent)
          .mapModel(m -> modelLens.set(model, m))
          .mapEffects(effectE -> Effect.map(effectE, eventPrism::embed));
    };
  }
}
