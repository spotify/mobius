/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
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
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/** Contains utility functions for working with {@link Connectables}. */
public final class Connectables {

  private Connectables() {
    // prevent instantiation
  }

  /**
   * Convert a {@code Connectable<I, O>} to a {@code Connectable<J, O>} by applying a function from
   * J to I for each J received, before invoking the {@code Connectable<I, O>}. This is a <a
   * href="https://hackage.haskell.org/package/contravariant-1.4.1/docs/Data-Functor-Contravariant.html">contravariant
   * functor</a> in functional programming terms.
   *
   * <p>This is useful for instance if you want your UI to use a subset or a transformed version of
   * the full model used in the loop. The returned {@link Connectable} doesn't enforce a connection
   * limit, but of course the connection limit of the wrapped {@link Connectable} applies.
   *
   * <p>As a simplified example, suppose that your model consists of a {@code Long} timestamp that
   * you want to format to a {@code String} before rendering it in the UI. Your UI could then
   * implement {@code Connectable<String, Event>}, and you could create a {@code Function<Long,
   * String>} that does the formatting. The {@link com.spotify.mobius.MobiusLoop} would be
   * outputting {@code Long} models that you need to convert to Strings before they can be accepted
   * by the UI.
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
    return new Connectable<J, O>() {
      @Nonnull
      @Override
      public Connection<J> connect(Consumer<O> output) throws ConnectionLimitExceededException {
        final Connection<I> delegateConnection = connectable.connect(output);

        return new Connection<J>() {
          @Override
          public void accept(J value) {
            delegateConnection.accept(mapper.apply(value));
          }

          @Override
          public void dispose() {
            delegateConnection.dispose();
          }
        };
      }
    };
  }
}
