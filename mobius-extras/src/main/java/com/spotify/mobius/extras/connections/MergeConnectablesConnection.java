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
package com.spotify.mobius.extras.connections;

import static com.spotify.mobius.internal_util.Preconditions.checkIterableNoNulls;
import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MergeConnectablesConnection<A, B> implements Connection<A> {

  private CopyOnWriteArrayList<Connection<A>> connections;

  public static <A, B> Connection<A> create(
      List<Connectable<A, B>> connectables, Consumer<B> output) {
    return new MergeConnectablesConnection<>(
        checkIterableNoNulls(connectables), checkNotNull(output));
  }

  public static <A, B> Connection<A> create(
      Connectable<A, B> fst, Connectable<A, B> snd, Consumer<B> output) {
    return create(Arrays.asList(checkNotNull(fst), checkNotNull(snd)), checkNotNull(output));
  }

  private MergeConnectablesConnection(List<Connectable<A, B>> connectables, Consumer<B> output) {
    List<Connection<A>> cs = new ArrayList<>(connectables.size());
    for (Connectable<A, B> connectable : connectables) {
      cs.add(connectable.connect(output));
    }

    connections = new CopyOnWriteArrayList<>(cs);
  }

  @Override
  public void accept(A value) {
    for (Connection<A> c : connections) {
      c.accept(value);
    }
  }

  @Override
  public void dispose() {
    List<Connection<A>> cs = new ArrayList<>(connections);
    connections.clear();
    for (Connection<A> c : cs) {
      c.dispose();
    }
  }
}
