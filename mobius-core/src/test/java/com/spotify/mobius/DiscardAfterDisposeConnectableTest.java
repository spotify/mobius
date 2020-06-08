/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class DiscardAfterDisposeConnectableTest {

  private RecordingConsumer<String> recordingConsumer;
  private Connection<Integer> connection;
  private TestConnection testConnection;

  private DiscardAfterDisposeConnectable<Integer, String> underTest;

  @Before
  public void setUp() throws Exception {
    recordingConsumer = new RecordingConsumer<>();
    testConnection = new TestConnection(recordingConsumer);

    underTest =
        new DiscardAfterDisposeConnectable<>(
            new Connectable<Integer, String>() {
              @Nonnull
              @Override
              public Connection<Integer> connect(Consumer<String> output) {
                return testConnection;
              }
            });
  }

  @Test
  public void nullActualThrowsNPE() throws Exception {
    assertThatThrownBy(() -> new DiscardAfterDisposeConnectable<Integer, String>(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void nullConsumerInConnectThrowsNPE() throws Exception {
    assertThatThrownBy(() -> underTest.connect(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void nullDisposableConsumerReturnedToConnectThrowsNPE() throws Exception {
    underTest =
        new DiscardAfterDisposeConnectable<>(
            new Connectable<Integer, String>() {
              @Nonnull
              @Override
              public Connection<Integer> connect(Consumer<String> output) {
                //noinspection ConstantConditions
                return null;
              }
            });

    assertThatThrownBy(() -> underTest.connect(recordingConsumer))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void forwardsMessagesToWrappedConsumer() throws Exception {
    connection = underTest.connect(recordingConsumer);
    connection.accept(14);
    recordingConsumer.assertValues("Value is: 14");
  }

  @Test
  public void delegatesDisposeToActualConnection() throws Exception {
    connection = underTest.connect(recordingConsumer);
    connection.dispose();
    assertThat(testConnection.disposed, is(true));
  }

  @Test
  public void discardsEventsAfterDisposal() throws Exception {
    // given a disposed connection
    connection = underTest.connect(recordingConsumer);
    connection.dispose();

    // when a message arrives
    connection.accept(1);

    // it is discarded
    recordingConsumer.assertValues();
  }

  private static class TestConnection implements Connection<Integer> {

    private boolean disposed;
    private final Consumer<String> eventConsumer;

    TestConnection(Consumer<String> eventConsumer) {
      this.eventConsumer = eventConsumer;
    }

    @Override
    public void accept(final Integer effect) {
      eventConsumer.accept("Value is: " + effect);
    }

    @Override
    public void dispose() {
      disposed = true;
    }
  }
}
