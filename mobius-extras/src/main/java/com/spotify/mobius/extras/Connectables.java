/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius.extras;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.extras.connections.ContramapConnection;
import com.spotify.mobius.extras.connections.DisconnectOnNullDimapConnection;
import com.spotify.mobius.extras.connections.MergeConnectablesConnection;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import com.spotify.mobius.internal_util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.Nonnull;

/** Contains utility functions for working with {@link Connectables}. */
public final class Connectables {

  private Connectables() {
    // prevent instantiation
  }

  /**
   * Convert a {@code Connectable<I, O>} to a {@code Connectable<J, O>} by applying the supplied
   * function from J to I for each J received, before passing it on to a {@code Connection<I>}
   * received from the underlying {@code Connectable<I, O>}. This makes {@link Connectable} a <a
   * href="https://hackage.haskell.org/package/contravariant-1.4.1/docs/Data-Functor-Contravariant.html">contravariant
   * functor</a> in functional programming terms.
   *
   * <p>The returned {@link Connectable} doesn't enforce a connection limit, but of course the
   * connection limit of the wrapped {@link Connectable} applies.
   *
   * <p>This is useful for instance if you want your UI to use a subset or a transformed version of
   * the full model used in the {@code MobiusLoop}. As a simplified example, suppose that your model
   * consists of a {@code Long} timestamp that you want to format to a {@code String} before
   * rendering it in the UI. Your UI could then implement {@code Connectable<String, Event>}, and
   * you could create a {@code NullValuedFunction<Long, String>} that does the formatting. The
   * {@link com.spotify.mobius.MobiusLoop} would be outputting {@code Long} models that you need to
   * convert to Strings before they can be accepted by the UI.
   *
   * <pre>
   * public class Formatter {
   *    public static String format(Long timestamp) { ... }
   * }
   *
   * public class MyUi implements Connectable<String, Event> {
   *    // other things in the UI implementation
   *
   *   {@literal @}Override
   *    public Connection<String> connect(Consumer<Event> output) {
   *       return new Connection<String>() {
   *        {@literal @}Override
   *         public void accept(String value) {
   *           // bind the value to the right UI element
   *         }
   *
   *        {@literal @}Override
   *         public void dispose() {
   *           // dispose of any resources, if needed
   *         }
   *       }
   *    }
   * }
   *
   * // Then, to connect the UI to a MobiusLoop.Controller with a Long model:
   * MobiusLoop.Controller<Long, Event> controller = ... ;
   * MyUi myUi = ... ;
   *
   * controller.connect(Connectables.contramap(Formatter::format, myUi));
   * </pre>
   *
   * @param mapper the mapping function to apply
   * @param connectable the underlying connectable
   * @param <I> the type the underlying connectable accepts
   * @param <J> the type the resulting connectable accepts
   * @param <O> the output type; usually the event type
   */
  @Nonnull
  public static <I, J, O> Connectable<J, O> contramap(
      final Function<J, I> mapper, final Connectable<I, O> connectable) {
    return SimpleConnectable.withConnectionFactory(
        new Function<Consumer<O>, Connection<J>>() {
          @Nonnull
          @Override
          public Connection<J> apply(Consumer<O> output) {
            return ContramapConnection.create(mapper, connectable, output);
          }
        });
  }

  /**
   * Convert a {@link Connectable} of one type pair to another type pair by converting every
   * incoming A to a B using the provided function. The function can then return either a B or null.
   * On the first B returned, a connection to the provided connectable is established, and that B is
   * passed through. If the aToB function returns null, this connection will be disposed. Whenever
   * the provided connectable dispatches a C through the consumer it receives, that C is converted
   * to a D and is then dispatched to whomever connected to the resulting connectable. The mechanism
   * described is the dimap function from Haskell's <a
   * href="http://hackage.haskell.org/package/profunctors-5.4/docs/Data-Profunctor.html#v:dimap">Profunctor
   * </a> typeclass
   *
   * <pre>
   *  class A {
   *      final B b;
   *      A(B b) { this.b = b; }
   *      B b() { return b }
   *  }
   *
   *  abstract class B {
   *      abstract int x();
   *  }
   *
   *  abstract class C {
   *      abstract String y();
   *  }
   *
   *  class D {
   *      final C c;
   *      D(C c) { this.c = c; }
   *  }
   *
   *  Connectable innerConnectable = o -> new Connection() {
   *      public void accept(B b) {
   *          o.accept(new C(b.x().toString()));
   *      }
   *
   *      public void dispose() {
   *
   *      }
   *  }
   *
   *  Connectable outerConnectable = dimap(A::b, D::new, innerConnectable);
   *  RecordingConsumer consumer = new RecordingConsumer<>();
   *  Connection connection = outerConnectable.connect(consumer);
   *  connection.accept(new A(new B(5))); // connects to innerConnectable and forwards value
   *  consumer.assertValues(new D(new C("5")))
   *
   *  connection.accept(new A(null)); // disconnects from innerConnectable
   *  connection.dispose(); // also disconnects from inner connectable
   * </pre>
   */
  @Nonnull
  public static <A, B, C, D> com.spotify.mobius.Connectable<A, D> dimap(
      final NullValuedFunction<A, B> aToB,
      final Function<C, D> cToD,
      final Connectable<B, C> connectable) {
    return SimpleConnectable.withConnectionFactory(
        new Function<Consumer<D>, Connection<A>>() {
          @Nonnull
          @Override
          public Connection<A> apply(Consumer<D> output) {
            return DisconnectOnNullDimapConnection.create(aToB, cToD, connectable, output);
          }
        });
  }

  /**
   * Merges all provided connectables into one. The resulting connectable will invoke all
   * connectables sequentially on connection, on receiving items, and will forward any generated
   * items to the receiver. Children with blocking connections must process incoming items on
   * separate threads or they will block the parent from dispatching inputs to the rest of the
   * siblings.
   */
  @SafeVarargs
  @Nonnull
  public static <A, B> Connectable<A, B> merge(
      final Connectable<A, B> fst, final Connectable<A, B> snd, final Connectable<A, B>... cs) {
    return SimpleConnectable.withConnectionFactory(
        new Function<Consumer<B>, Connection<A>>() {
          @Nonnull
          @Override
          public Connection<A> apply(Consumer<B> output) {
            final ArrayList<Connectable<A, B>> connectables = new ArrayList<>(cs.length + 2);
            connectables.add(fst);
            connectables.add(snd);
            Collections.addAll(
                connectables, (Connectable<A, B>[]) Preconditions.checkArrayNoNulls(cs));
            return MergeConnectablesConnection.create(connectables, output);
          }
        });
  }
}
