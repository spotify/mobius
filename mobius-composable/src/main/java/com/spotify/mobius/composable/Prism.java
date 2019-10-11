package com.spotify.mobius.composable;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.extras.NullValuedFunction;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A simple functional lens, used to point at something that may or may not be present.
 *
 * <p>You can think of this as a filter - it will point at a specific case, and if it's not there,
 * you won't be able to read it.
 *
 * <p>The typical use-case for this is to point at a specific dataenum case, and only read out the
 * data of a dataenum value if it is that specific case.
 */
public final class Prism<S, A> {

  @Nonnull private final NullValuedFunction<S, A> extract;
  @Nonnull private final Function<A, S> embed;

  private Prism(NullValuedFunction<S, A> extract, Function<A, S> embed) {
    this.extract = checkNotNull(extract);
    this.embed = checkNotNull(embed);
  }

  /** Tries to read the value that this prism is pointing at if possible, otherwise returns null. */
  @Nullable
  public A extract(S struct) {
    return extract.apply(struct);
  }

  /**
   * Wraps the given value in a new instance of the outer structure.
   *
   * <p>In the case of a dataenum like this, and a Prism&lt;Foo, Integer&gt;
   *
   * <pre>
   * interface Foo_dataenum  {
   *   dataenum_case Bar(Integer x);
   * }
   * </pre>
   *
   * Calling prism.embed(3) would return a Foo, containing a Bar(3).
   */
  @Nonnull
  public S embed(A value) {
    return checkNotNull(embed.apply(value));
  }

  /** Chain together two prisms S -> A and A -> B into a single prism S -> B */
  public <X> Prism<S, X> compose(Prism<A, X> other) {
    checkNotNull(other);
    return new Prism<>(
        s -> {
          A a = extract(s);
          if (a == null) {
            return null;
          }
          return other.extract(a);
        },
        x -> embed(other.embed(x)));
  }

  /** Create a new prism from the given two functions. */
  public static <S, A> Prism<S, A> prism(NullValuedFunction<S, A> extract, Function<A, S> embed) {
    return new Prism<>(extract, embed);
  }
}
