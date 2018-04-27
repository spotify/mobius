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

import com.spotify.mobius.functions.Function;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class ConnectablesTest {

  private TestConsumer<String> outputConsumer;

  @Before
  public void setUp() throws Exception {
    outputConsumer = new TestConsumer<>();
  }

  @Test
  public void fromRunnableShouldApplyGivenRunnable() throws Exception {
    final AtomicBoolean yesIRan = new AtomicBoolean();

    Connectables.<Integer, String>fromRunnable(
            new Runnable() {
              @Override
              public void run() {
                yesIRan.set(true);
              }
            })
        .connect(outputConsumer)
        .accept(1);

    assertThat(yesIRan.get()).isTrue();
    assertThat(outputConsumer.received).isEmpty();
  }

  @Test
  public void fromConsumerShouldApplyGivenConsumer() throws Exception {
    TestConsumer<Integer> consumer = new TestConsumer<>();

    Connectables.<Integer, String>fromConsumer(consumer).connect(outputConsumer).accept(8735);

    assertThat(consumer.received).containsOnly(8735);
    assertThat(outputConsumer.received).isEmpty();
  }

  @Test
  public void fromFunctionShouldApplyGivenFunctionAndPropagateEvents() throws Exception {

    Connectables.fromFunction(
            new Function<Integer, String>() {
              @Nonnull
              @Override
              public String apply(Integer value) {
                return String.valueOf(value);
              }
            })
        .connect(outputConsumer)
        .accept(8735);

    assertThat(outputConsumer.received).containsExactly("8735");
  }

  @Test
  public void shouldThrowNpeForNullRunnable() throws Exception {
    assertThatThrownBy(() -> Connectables.fromRunnable(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowNpeForNullConsumer() throws Exception {
    assertThatThrownBy(() -> Connectables.fromConsumer(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldThrowNpeForNullFunction() throws Exception {
    assertThatThrownBy(() -> Connectables.fromFunction(null))
        .isInstanceOf(NullPointerException.class);
  }
}
