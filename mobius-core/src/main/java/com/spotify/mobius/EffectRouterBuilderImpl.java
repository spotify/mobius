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

import com.spotify.mobius.functions.BiConsumer;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

class EffectRouterBuilderImpl<F, E> implements EffectRouterBuilder<F, E> {

  private final List<Connectable<F, E>> connectables;
  private final List<Class<?>> registeredClasses;
  private BiConsumer<F, Throwable> errorHandler =
      new BiConsumer<F, Throwable>() {
        @Override
        public void accept(F value, Throwable value2) {
          throw new ConnectionException(value, value2);
        }
      };

  EffectRouterBuilderImpl() {
    connectables = new ArrayList<>();
    registeredClasses = new ArrayList<>();
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addRunnable(
      Class<G> effectClass, final Runnable action) {
    checkNotNull(action);

    return addConnectable(
        effectClass,
        new Connectable<G, E>() {
          @Nonnull
          @Override
          public Connection<G> connect(Consumer<E> output) throws ConnectionLimitExceededException {
            return new Connection<G>() {
              @Override
              public void accept(G value) {
                action.run();
              }

              @Override
              public void dispose() {}
            };
          }
        });
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addConsumer(
      Class<G> effectClass, final Consumer<G> consumer) {
    checkNotNull(consumer);

    return addConnectable(
        effectClass,
        new Connectable<G, E>() {
          @Nonnull
          @Override
          public Connection<G> connect(Consumer<E> output) throws ConnectionLimitExceededException {
            return new Connection<G>() {
              @Override
              public void accept(G value) {
                consumer.accept(value);
              }

              @Override
              public void dispose() {}
            };
          }
        });
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addFunction(
      Class<G> effectClass, final Function<G, E> function) {
    checkNotNull(function);

    return addConnectable(
        effectClass,
        new Connectable<G, E>() {
          @Nonnull
          @Override
          public Connection<G> connect(final Consumer<E> output)
              throws ConnectionLimitExceededException {
            return new Connection<G>() {
              @Override
              public void accept(G value) {
                output.accept(function.apply(value));
              }

              @Override
              public void dispose() {}
            };
          }
        });
  }

  @Override
  public <G extends F> EffectRouterBuilder<F, E> addConnectable(
      Class<G> effectClass, Connectable<G, E> connectable) {
    validateAndTrackeffectClass(effectClass);

    connectables.add(new SubtypeFilteringConnectable<>(effectClass, connectable));

    return this;
  }

  @Override
  public EffectRouterBuilder<F, E> withFatalErrorHandler(BiConsumer<F, Throwable> errorHandler) {

    this.errorHandler = checkNotNull(errorHandler);
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

  private class SubtypeFilteringConnectable<G extends F> implements Connectable<F, E> {
    private final Class<G> handledClass;
    private final Connectable<G, E> delegate;

    SubtypeFilteringConnectable(Class<G> handledClass, Connectable<G, E> delegate) {
      this.handledClass = handledClass;
      this.delegate = delegate;
    }

    @Nonnull
    @Override
    public Connection<F> connect(Consumer<E> output) throws ConnectionLimitExceededException {
      final Connection<G> delegateConnection = delegate.connect(checkNotNull(output));

      return new Connection<F>() {
        @Override
        public void accept(F value) {
          if (handledClass.isInstance(value)) {
            try {
              delegateConnection.accept(handledClass.cast(value));
            } catch (Exception e) {
              errorHandler.accept(value, e);
            }
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
