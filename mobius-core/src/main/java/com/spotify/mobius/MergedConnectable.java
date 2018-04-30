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

/**
 * A connectable that combines a number of sub-connectables. On {@link #connect(Consumer)}, all
 * sub-connectables are connected to the output {@link Consumer} and the {@link Connection} returned
 * by this connectable forwards all inputs to each of the sub-connections.
 *
 * <p>NOTE: this class is a potential candidate for making into a part of the public API of Mobius.
 * That would require some more understanding of what the implications are, and probably some more
 * thought about the name. 'Merged' is maybe a bit too general for what this does, although it's
 * hard to think of a better name. 'Branching', 'Splitting', 'Broadcast', 'FanOutFanIn',
 * 'SplitAndMerge' come to mind as ideas.
 */
class MergedConnectable<I, O> implements Connectable<I, O> {

  private final List<Connectable<I, O>> connectables;

  private MergedConnectable(List<Connectable<I, O>> connectables) {
    this.connectables = connectables;
  }

  /**
   * @param output the consumer that the new connection should use to emit values
   * @return a new {@link Connection} that sends each input value to all connections resulting from
   *     invoking {@link Connectable#connect(Consumer)} on the sub-connectables with {@code output}
   *     parameter.
   * @throws ConnectionLimitExceededException if any sub-connectable does
   */
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

  public static <I, O> MergedConnectable<I, O> create(
      Iterable<? extends Connectable<I, O>> connectables) {
    final List<Connectable<I, O>> list = ImmutableUtil.immutableList(connectables);

    if (list.isEmpty()) {
      throw new IllegalArgumentException("Connectables collection must be non-empty");
    }

    return new MergedConnectable<>(list);
  }
}
