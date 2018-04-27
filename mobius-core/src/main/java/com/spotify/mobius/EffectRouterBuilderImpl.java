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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** TODO: document! */
class EffectRouterBuilderImpl<F, E> implements EffectRouterBuilder<F, E> {

  private final List<Connectable<F, E>> connectables;
  private final List<Class<?>> registeredClasses;

  EffectRouterBuilderImpl() {
    connectables = new ArrayList<>();
    registeredClasses = new ArrayList<>();
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addRunnable(
      Class<G> effectClass, Runnable action) {
    return addConnectable(effectClass, Connectables.<G, E>fromRunnable(action));
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addConsumer(
      Class<G> effectClass, Consumer<G> consumer) {
    return addConnectable(effectClass, Connectables.<G, E>fromConsumer(consumer));
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addFunction(
      Class<G> effectClass, Function<G, E> function) {
    return addConnectable(effectClass, Connectables.fromFunction(function));
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addConnectable(
      Class<G> effectClass, Connectable<G, E> connectable) {
    validateAndTrackeffectClass(effectClass);

    connectables.add(new ClassFilteringConnectable<F, G, E>(effectClass, connectable));

    return this;
  }

  private <G extends F> void validateAndTrackeffectClass(Class<G> effectClass) {
    for (Class<?> existing : registeredClasses) {
      if (effectClass.isAssignableFrom(existing) || existing.isAssignableFrom(effectClass)) {
        throw new IllegalArgumentException(
            "Effect classes must not be assignable to each other, "
                + effectClass.getName()
                + " collides with existing: "
                + existing.getName());
      }
    }

    registeredClasses.add(effectClass);
  }

  @Override
  public Connectable<F, E> build() {
    connectables.add(new UnknownEffectReportingConnectable<F, E>(registeredClasses));

    return new SafeConnectable<>(MergedConnectable.create(connectables));
  }

  private static class ClassFilteringConnectable<I, J extends I, O> implements Connectable<I, O> {
    private final Class<J> handledClass;
    private final Connectable<J, O> delegate;

    ClassFilteringConnectable(Class<J> handledClass, Connectable<J, O> delegate) {
      this.handledClass = handledClass;
      this.delegate = delegate;
    }

    @Nonnull
    @Override
    public Connection<I> connect(Consumer<O> output) throws ConnectionLimitExceededException {
      final Connection<J> delegateConnection = delegate.connect(checkNotNull(output));

      return new Connection<I>() {
        @Override
        public void accept(I value) {
          if (handledClass.isInstance(value)) {
            delegateConnection.accept(handledClass.cast(value));
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
