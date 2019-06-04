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
  public void shouldProcessInitBeforeEventsFromConnectableEventSource() throws Exception {
    mobiusStore = MobiusStore.create(m -> First.first("First" + m), update, "init");

    eventSource =
        new Connectable<String, TestEvent>() {

          @Nonnull
          @Override
          public Connection<String> connect(Consumer<TestEvent> eventConsumer) {
            eventConsumer.accept(new TestEvent("1"));
            return new Connection<String>() {
              @Override
              public void accept(String value) {}

              @Override
              public void dispose() {}
            };
          }
        };

    setupWithEffects(new FakeEffectHandler(), immediateRunner);

    // in this scenario, the init and the first event get processed before the observer
    // is connected, meaning the 'Firstinit' state is never seen
    observer.assertStates("Firstinit->1");
  }

  @Test
  public void shouldProcessInitBeforeEventsFromEventSource() throws Exception {
    mobiusStore = MobiusStore.create(m -> First.first("First" + m), update, "init");

    eventSource =
        EventSourceConnectable.create(
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
            });

    setupWithEffects(new FakeEffectHandler(), immediateRunner);

    // in this scenario, the init and the first event get processed before the observer
    // is connected, meaning the 'Firstinit' state is never seen
    observer.assertStates("Firstinit->1");
  }
}
