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
package com.spotify.mobius.android;

import android.os.Bundle;
import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MappingMobiusController<M, V, E> implements MobiusController<V, E> {

  private final Function<M, V> viewDataMapper;
  private final MobiusController<M, E> delegate;

  MappingMobiusController(MobiusController<M, E> delegate, Function<M, V> viewDataMapper) {
    this.delegate = delegate;
    this.viewDataMapper = viewDataMapper;
  }

  @Override
  public boolean isRunning() {
    return delegate.isRunning();
  }

  @Override
  public void connect(final Connectable<V, E> view) {
    Connectable<M, E> modelConnectable =
        new Connectable<M, E>() {
          @Nonnull
          @Override
          public Connection<M> connect(Consumer<E> output) {
            final Connection<V> viewDataRenderer = view.connect(output);

            return new Connection<M>() {
              @Override
              public void accept(M model) {
                V viewData = viewDataMapper.apply(model);
                viewDataRenderer.accept(viewData);
              }

              @Override
              public void dispose() {
                viewDataRenderer.dispose();
              }
            };
          }
        };

    delegate.connect(modelConnectable);
  }

  @Override
  public void disconnect() {
    delegate.disconnect();
  }

  @Override
  public void start() {
    delegate.start();
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  @Override
  public void restoreState(@Nullable Bundle in) {
    delegate.restoreState(in);
  }

  @Override
  public void saveState(@Nullable Bundle out) {
    delegate.restoreState(out);
  }
}
