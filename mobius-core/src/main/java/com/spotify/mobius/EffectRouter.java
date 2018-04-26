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
package com.spotify.mobius;

import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** TODO: document! TODO: is this the merged connectable? Or does it have one? */
class EffectRouter<F, E> implements Connectable<F, E> {

  private final MergedConnectable<F, E> mergedConnectable;

  private EffectRouter(MergedConnectable<F, E> mergedConnectable) {
    this.mergedConnectable = mergedConnectable;
  }

  @Nonnull
  @Override
  public Connection<F> connect(Consumer<E> output) throws ConnectionLimitExceededException {
    // TODO: concurrency/synchronisation of writes to output!
    return mergedConnectable.connect(output);
  }

  public static <F, E> Builder<F, E> builder() {
    return new Builder<>();
  }

  static class Builder<F, E> implements EffectRouterBuilder<F, E> {

    private final List<Connectable<F, E>> connectables;
    private final List<Class<?>> classes;

    Builder() {
      connectables = new ArrayList<>();
      classes = new ArrayList<>();
    }

    @Override
    public <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Runnable action) {
      return add(klazz, Connectables.<G, E>fromRunnable(action));
    }

    @Override
    public <G extends F> EffectRouterBuilder<F, E> add(Class<G> klazz, Consumer<G> consumer) {
      return add(klazz, Connectables.<G, E>fromConsumer(consumer));
    }

    @Override
    public <G extends F> EffectRouterBuilder<F, E> add(
        Class<G> klazz, Connectable<G, E> connectable) {
      validateAndTrackClass(klazz);

      connectables.add(new FilteredConnectable<F, G, E>(klazz, connectable));

      return this;
    }

    private <G extends F> void validateAndTrackClass(Class<G> klazz) {
      for (Class<?> existing : classes) {
        if (klazz.isAssignableFrom(existing) || existing.isAssignableFrom(klazz)) {
          throw new IllegalArgumentException(
              "Effect classes may not be assignable to each other, "
                  + klazz.getName()
                  + " collides with existing: "
                  + existing.getName());
        }
      }

      classes.add(klazz);
    }

    @Override
    public Connectable<F, E> build() {
      connectables.add(new UnknownEffectReportingConnectable<F, E>(classes));

      return new SafeConnectable<>(MergedConnectable.create(connectables));
    }
  }

  private static class FilteredConnectable<I, J extends I, O> implements Connectable<I, O> {
    private final Class<J> klazz;
    private final Connectable<J, O> delegate;

    FilteredConnectable(Class<J> klazz, Connectable<J, O> delegate) {
      this.klazz = klazz;
      this.delegate = delegate;
    }

    @Nonnull
    @Override
    public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
      final Connection<J> delegateConnection = delegate.connect(output);

      return new Connection<I>() {
        @Override
        public void accept(I value) {
          if (klazz.isInstance(value)) {
            delegateConnection.accept(klazz.cast(value));
          }
          // ignore
        }

        @Override
        public void dispose() {
          delegateConnection.dispose();
        }
      };
    }
  }
}
