package com.spotify.mobius.composable;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.functions.BiFunction;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/**
 * A simple functional lens, used to read and write values in nested POJOs.
 *
 * <p>A lens essentially is a reference to a field inside a structure, and allows you to read,
 * write, or manipulate that field. The real strength is that when you update a field in an
 * immutable structure, the lens will help you create the new outer S with your changes applied.
 */
public final class Lens<S, A> {

  private final Function<S, A> get;
  private final BiFunction<S, A, S> set;

  private Lens(Function<S, A> get, BiFunction<S, A, S> set) {
    this.get = checkNotNull(get);
    this.set = checkNotNull(set);
  }

  /** Read out the value that this lens is pointing at from a given S. */
  @Nonnull
  public A get(S struct) {
    return checkNotNull(get.apply(checkNotNull(struct)));
  }

  /**
   * Set the value that this lens is pointing at in a given S. Returns a new S with the change
   * applied.
   */
  @Nonnull
  public S set(S struct, A value) {
    return checkNotNull(set.apply(checkNotNull(struct), checkNotNull(value)));
  }

  /**
   * Apply a function to the value that this lens is pointing at in a given S. Returns a new S with
   * the change applied.
   */
  @Nonnull
  public S modify(S struct, Function<A, A> f) {
    return set(struct, checkNotNull(f).apply(get(struct)));
  }

  /** Chain together two lenses S -> A and A -> B into a single lens S -> B */
  @Nonnull
  public <B> Lens<S, B> compose(Lens<A, B> other) {
    checkNotNull(other);
    return new Lens<>(
        s -> other.get(get(s)),
        (s, x) -> {
          A a = get(s);
          A newA = other.set(a, x);
          return set(s, newA);
        });
  }

  /** Create a new lens from the given two functions. */
  @Nonnull
  public static <S, A> Lens<S, A> lens(Function<S, A> get, BiFunction<S, A, S> set) {
    return new Lens<>(get, set);
  }
}
