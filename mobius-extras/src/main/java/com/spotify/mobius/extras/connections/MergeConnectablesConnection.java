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

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.functions.Consumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MergeConnectablesConnection<A, B> implements Connection<A> {

  private final List<Connection<A>> connections;

  public static <A, B> Connection<A> create(
      List<Connectable<A, B>> connectables, Consumer<B> output) {
    return new MergeConnectablesConnection<>(connectables, output);
  }

  public static <A, B> Connection<A> create(
      Connectable<A, B> fst, Connectable<A, B> snd, Consumer<B> output) {
    return create(Arrays.asList(fst, snd), output);
  }

  private MergeConnectablesConnection(List<Connectable<A, B>> connectables, Consumer<B> output) {
    connections = new ArrayList<>(connectables.size());
    for (Connectable<A, B> connectable : connectables) {
      connections.add(connectable.connect(output));
    }
  }

  @Override
  public synchronized void accept(A value) {
    for (Connection<A> c : connections) {
      c.accept(value);
    }
  }

  @Override
  public synchronized void dispose() {
    for (Connection<A> c : connections) {
      c.dispose();
    }
  }
}
