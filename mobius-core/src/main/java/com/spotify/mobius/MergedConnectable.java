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
import com.spotify.mobius.internal_util.ImmutableUtil;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** TODO: document! */
class MergedConnectable<I, O> implements Connectable<I, O> {

  private final List<Connectable<I, O>> connectables;

  private MergedConnectable(List<Connectable<I, O>> connectables) {
    this.connectables = connectables;
  }

  @Nonnull
  @Override
  public synchronized Connection<I> connect(final Consumer<O> output)
      throws ConnectionLimitExceededException {

    final List<Connection<I>> connections = new ArrayList<>(connectables.size());

    for (Connectable<I, O> connectable : connectables) {
      connections.add(connectable.connect(output));
    }

    return new Connection<I>() {
      @Override
      public void accept(I value) {
        for (Connection<I> connection : connections) {
          connection.accept(value);
        }
      }

      @Override
      public void dispose() {
        for (Connection<I> connection : connections) {
          connection.dispose();
        }
      }
    };
  }

  public static <I, O> MergedConnectable<I, O> create(Iterable<Connectable<I, O>> connectables) {
    final List<Connectable<I, O>> list = ImmutableUtil.immutableList(connectables);

    if (list.isEmpty()) {
      throw new IllegalArgumentException("Connectables collection must be non-empty");
    }

    return new MergedConnectable<>(list);
  }
}
