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
package com.spotify.mobius.rx3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class DiscardAfterDisposeConnectableTest {

  private RecordingConsumer<String> recordingConsumer;
  private Connection<Integer> connection;
  private Semaphore blockEffectPerformer;
  private Semaphore signalEffectHasBeenPerformed;
  private BlockableConnection blockableConnection;

  private DiscardAfterDisposeConnectable<Integer, String> underTest;

  private final ExecutorService executorService = Executors.newCachedThreadPool();

  @Before
  public void setUp() throws Exception {
    blockEffectPerformer = new Semaphore(0);
    signalEffectHasBeenPerformed = new Semaphore(0);

    recordingConsumer = new RecordingConsumer<>();
    blockableConnection = new BlockableConnection(recordingConsumer);

    underTest =
        new DiscardAfterDisposeConnectable<>(
            new Connectable<Integer, String>() {
              @Nonnull
              @Override
              public Connection<Integer> connect(Consumer<String> output) {
                return blockableConnection;
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
  public void delegatesEffectsToActualSink() throws Exception {
    connection = underTest.connect(recordingConsumer);
    connection.accept(1);
    recordingConsumer.assertValues("Value is: 1");
  }

  @Test
  public void delegatesDisposeToActualSink() throws Exception {
    connection = underTest.connect(recordingConsumer);
    connection.dispose();
    assertThat(blockableConnection.disposed, is(true));
  }

  @Test
  public void discardsEventsAfterDisposal() throws Exception {
    connection = underTest.connect(recordingConsumer);

    // given the effect performer is blocked
    blockableConnection.block = true;

    // when an effect is requested
    Future<?> effectPerformedFuture = executorService.submit(() -> connection.accept(1));

    // and the sink is disposed
    connection.dispose();

    // before the effect gets performed
    // (needs permitting the blocked effect performer to proceed)
    blockEffectPerformer.release();

    // (get the result of the future to ensure the effect has been performed, also propagating
    // exceptions if any - result should happen quickly, but it's good to have a timeout in case
    // something is messed up)
    effectPerformedFuture.get(10, TimeUnit.SECONDS);

    // then no events are emitted
    recordingConsumer.assertValues();
  }

  @Test
  public void discardsEffectsAfterDisposal() throws Exception {
    // given a disposed sink
    connection = underTest.connect(recordingConsumer);
    connection.dispose();

    // when an effect is performed
    connection.accept(1);

    // then no effects or events happen
    blockableConnection.assertEffects();
    recordingConsumer.assertValues();
  }

  private class BlockableConnection implements Connection<Integer> {

    private final List<Integer> recordedEffects = new ArrayList<>();
    private boolean disposed;
    private final Consumer<String> eventConsumer;
    private volatile boolean block = false;

    BlockableConnection(Consumer<String> eventConsumer) {
      this.eventConsumer = eventConsumer;
    }

    void assertEffects(Integer... values) {
      assertThat(recordedEffects, equalTo(Arrays.asList(values)));
    }

    @Override
    public void accept(final Integer effect) {
      if (block) {
        try {
          if (!blockEffectPerformer.tryAcquire(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("timed out waiting for effect performer unblock");
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      recordedEffects.add(effect);
      eventConsumer.accept("Value is: " + effect);
      signalEffectHasBeenPerformed.release();
    }

    @Override
    public void dispose() {
      disposed = true;
      signalEffectHasBeenPerformed.release();
    }
  }
}
