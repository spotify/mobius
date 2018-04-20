package com.spotify.mobius;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;


import com.spotify.mobius.functions.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO: document!
 */
public class ConnectablesTest {

  private TestConsumer<String> outputConsumer;

  @Before
  public void setUp() throws Exception {
    outputConsumer = new TestConsumer<>();
  }

  @Test
  public void fromRunnableShouldApplyGivenRunnable() throws Exception {
    final AtomicBoolean yesIRan = new AtomicBoolean();

    Connectables.<Integer, String>fromRunnable(new Runnable() {
      @Override
      public void run() {
        yesIRan.set(true);
      }
    }).connect(outputConsumer).accept(1);

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
}