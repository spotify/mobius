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

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableList;
import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class ConnectablesTest {
  private IntegerConsumer output;
  private TestConnectable delegate;
  private AtomicReference<Double> mapParameter;
  private Connectable<Double, Integer> mapped;

  @Before
  public void setUp() throws Exception {
    output = new IntegerConsumer();
    delegate = new TestConnectable();
    mapParameter = new AtomicReference<>();
    mapped =
        Connectables.contramap(
            new Function<Double, String>() {
              @Nonnull
              @Override
              public String apply(Double value) {
                mapParameter.set(value);
                return String.valueOf(value);
              }
            },
            delegate);
  }

  @Test
  public void shouldApplyMappingFunctionToIncoming() throws Exception {
    mapped.connect(output).accept(98.0);

    assertThat(mapParameter.get(), is(98.0));
  }

  @Test
  public void shouldPropagateToOutgoing() throws Exception {
    mapped.connect(output).accept(101.0);

    assertThat(output.received(), is(singletonList(5)));
  }

  @Test
  public void shouldPropagateDispose() throws Exception {
    mapped.connect(output).dispose();

    assertThat(delegate.isDisposed(), is(true));
  }

  private static class TestConnectable implements Connectable<String, Integer> {
    private volatile boolean connected = false;
    private volatile boolean disposed = false;

    @Nonnull
    @Override
    public Connection<String> connect(final Consumer<Integer> output)
        throws ConnectionLimitExceededException {
      // not race free, but should be fine in practice with the tests we've got right now
      if (connected) {
        throw new ConnectionLimitExceededException();
      }
      connected = true;

      return new Connection<String>() {

        @Override
        public void accept(String value) {
          output.accept(value.length());
        }

        @Override
        public void dispose() {
          disposed = true;
        }
      };
    }

    public boolean isDisposed() {
      return disposed;
    }
  }

  private static class IntegerConsumer implements Consumer<Integer> {

    private final List<Integer> received = new CopyOnWriteArrayList<>();

    @Override
    public void accept(Integer value) {
      received.add(value);
    }

    List<Integer> received() {
      return ImmutableList.copyOf(received);
    }
  }
}
