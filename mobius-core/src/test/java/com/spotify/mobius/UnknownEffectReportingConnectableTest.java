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
import org.junit.Before;
import org.junit.Test;

public class UnknownEffectReportingConnectableTest {

  private TestConsumer<Integer> consumer;

  private Connection<CharSequence> connection;

  @Before
  public void setUp() throws Exception {
    consumer = new TestConsumer<>();

    UnknownEffectReportingConnectable<CharSequence, Integer> connectable =
        new UnknownEffectReportingConnectable<>(
            ImmutableList.<Class<?>>of(String.class, StringBuilder.class));

    connection = connectable.connect(consumer);
  }

  @Test
  public void shouldIgnoreFirstHandledClass() throws Exception {
    connection.accept("hey");

    assertThat(consumer.received).isEmpty();
  }

  @Test
  public void shouldIgnoreSecondHandledClass() throws Exception {
    connection.accept(new StringBuilder("hoo"));

    assertThat(consumer.received).isEmpty();
  }

  @Test
  public void shouldThrowForUnhandledClasses() throws Exception {
    final StringBuffer stringBuffer = new StringBuffer("hi");
    assertThatThrownBy(
            () -> {
              connection.accept(stringBuffer);
            })
        .isInstanceOf(UnknownEffectException.class)
        .hasFieldOrPropertyWithValue("effect", stringBuffer);
  }
}
