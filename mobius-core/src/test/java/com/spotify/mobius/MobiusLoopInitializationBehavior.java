package com.spotify.mobius;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.SimpleConnection;
import com.spotify.mobius.testdomain.TestEffect;
import com.spotify.mobius.testdomain.TestEvent;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusLoopInitializationBehavior extends MobiusLoopTest {
  @Test
  public void shouldProcessInitBeforeEventsFromEffectHandler() throws Exception {
    mobiusStore = MobiusStore.create(m -> First.first("I" + m), update, "init");

    // when an effect handler that emits events before returning the connection
    setupWithEffects(
        new Connectable<TestEffect, TestEvent>() {
          @Nonnull
          @Override
          public Connection<TestEffect> connect(Consumer<TestEvent> output)
              throws ConnectionLimitExceededException {
            output.accept(new TestEvent("1"));

            return new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect value) {
                // do nothing
              }
            };
          }
        },
        immediateRunner);

    // in this scenario, the init and the first event get processed before the observer
    // is connected, meaning the 'Iinit' state is never seen
    observer.assertStates("Iinit->1");
  }

  @Test
  public void shouldProcessInitBeforeEventsFromEventSource() throws Exception {
    mobiusStore = MobiusStore.create(m -> First.first("First" + m), update, "init");

    eventSource =
        new EventSource<TestEvent>() {
          @Nonnull
          @Override
          public Disposable subscribe(Consumer<TestEvent> eventConsumer) {
            eventConsumer.accept(new TestEvent("1"));
            return new Disposable() {
              @Override
              public void dispose() {
                // do nothing
              }
            };
          }
        };

    setupWithEffects(new FakeEffectHandler(), immediateRunner);

    // in this scenario, the init and the first event get processed before the observer
    // is connected, meaning the 'Firstinit' state is never seen
    observer.assertStates("Firstinit->1");
  }
}
