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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;

/** A {@link Connection} implementation that does contravariant mapping. */
public class ContramapConnection<A, B, C> implements Connection<B> {

  public static <A, B, C> Connection<B> create(
      Function<B, A> mapper, Connectable<A, C> connectable, Consumer<C> output) {
    return new ContramapConnection<>(
        checkNotNull(mapper), checkNotNull(connectable), checkNotNull(output));
  }

  private final Function<B, A> mapper;
  private final Connectable<A, C> connectable;
  private final Consumer<C> output;
  private final Connection<A> delegateConnection;

  private ContramapConnection(
      Function<B, A> mapper, Connectable<A, C> connectable, Consumer<C> output) {
    this.mapper = mapper;
    this.connectable = connectable;
    this.output = output;

    delegateConnection = this.connectable.connect(this.output);
  }

  @Override
  public void accept(B j) {
    final A a = mapper.apply(j);
    if (a != null) {
      delegateConnection.accept(a);
    }
  }

  @Override
  public void dispose() {
    delegateConnection.dispose();
  }
}
