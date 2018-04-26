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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.SimpleConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class MergedConnectableTest {

  private AtomicInteger result;
  private AtomicBoolean secondMatched;
  private Connection<String> connection;

  @Before
  public void setUp() throws Exception {

    List<Connectable<String, Integer>> connectables = new ArrayList<>();

    connectables.add(
        new Connectable<String, Integer>() {
          @Nonnull
          @Override
          public Connection<String> connect(final Consumer<Integer> output) {
            return new SimpleConnection<String>() {
              @Override
              public void accept(String value) {
                output.accept(value.length());
              }
            };
          }
        });
    connectables.add(
        new Connectable<String, Integer>() {
          @Nonnull
          @Override
          public Connection<String> connect(final Consumer<Integer> output) {
            return new SimpleConnection<String>() {
              @Override
              public void accept(String value) {
                secondMatched.set(true);
              }
            };
          }
        });

    result = new AtomicInteger(new Random().nextInt());
    secondMatched = new AtomicBoolean(false);

    connection = MergedConnectable.create(connectables).connect(i -> result.set(i));
  }

  @Test
  public void shouldSendToAllConnectables() throws Exception {
    connection.accept("hiya there");
    assertThat(result.get()).isEqualTo(10);
    assertThat(secondMatched.get()).isTrue();
  }

  @Test
  public void shouldDisallowEmptyConfig() throws Exception {
    assertThatThrownBy(
            () -> MergedConnectable.create(Collections.<Connectable<String, Integer>>emptyList()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void connectionsShouldDisposeSubConnections() throws Exception {
    final DisposableConnection<String> c1 = new DisposableConnection<>();
    final DisposableConnection<String> c2 = new DisposableConnection<>();

    Connectable<String, Integer> connectable1 =
        new Connectable<String, Integer>() {
          @Nonnull
          @Override
          public Connection<String> connect(Consumer<Integer> output)
              throws ConnectionLimitExceededException {
            return c1;
          }
        };
    Connectable<String, Integer> connectable2 =
        new Connectable<String, Integer>() {
          @Nonnull
          @Override
          public Connection<String> connect(Consumer<Integer> output)
              throws ConnectionLimitExceededException {
            return c2;
          }
        };
    Connection<String> connection =
        MergedConnectable.create(ImmutableList.of(connectable1, connectable2)).connect(i -> {});

    connection.dispose();

    assertThat(c1.disposed).isTrue();
    assertThat(c2.disposed).isTrue();
  }

  private static class DisposableConnection<T> implements Connection<T> {

    private volatile boolean disposed;

    @Override
    public void accept(T value) {
      // empty
    }

    @Override
    public void dispose() {
      disposed = true;
    }
  }
}
