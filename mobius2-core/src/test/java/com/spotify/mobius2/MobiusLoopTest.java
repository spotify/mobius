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
package com.spotify.mobius2;

import static com.spotify.mobius2.Effects.effects;

import com.spotify.mobius2.functions.Consumer;
import com.spotify.mobius2.internal_util.ImmutableUtil;
import com.spotify.mobius2.runners.ExecutorServiceWorkRunner;
import com.spotify.mobius2.runners.ImmediateWorkRunner;
import com.spotify.mobius2.runners.WorkRunner;
import com.spotify.mobius2.test.RecordingConsumer;
import com.spotify.mobius2.test.RecordingModelObserver;
import com.spotify.mobius2.test.SimpleConnection;
import com.spotify.mobius2.testdomain.Crash;
import com.spotify.mobius2.testdomain.EventWithCrashingEffect;
import com.spotify.mobius2.testdomain.EventWithSafeEffect;
import com.spotify.mobius2.testdomain.SafeEffect;
import com.spotify.mobius2.testdomain.TestEffect;
import com.spotify.mobius2.testdomain.TestEvent;
import java.util.Set;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Before;

public class MobiusLoopTest {

  com.spotify.mobius2.MobiusLoop<String, TestEvent, TestEffect> mobiusLoop;
  com.spotify.mobius2.Connectable<TestEffect, TestEvent> effectHandler;

  final WorkRunner immediateRunner = new ImmediateWorkRunner();
  WorkRunner backgroundRunner;

  com.spotify.mobius2.Connectable<String, TestEvent> eventSource =
      new com.spotify.mobius2.Connectable<String, TestEvent>() {
        @Nonnull
        @Override
        public com.spotify.mobius2.Connection<String> connect(Consumer<TestEvent> output) {
          return new com.spotify.mobius2.Connection<String>() {
            @Override
            public void accept(String value) {}

            @Override
            public void dispose() {}
          };
        }
      };

  RecordingModelObserver<String> observer;
  RecordingConsumer<TestEffect> effectObserver;

  com.spotify.mobius2.Update<String, TestEvent, TestEffect> update;
  String startModel;
  Set<TestEffect> startEffects;

  @Before
  public void setUp() throws Exception {
    backgroundRunner = new ExecutorServiceWorkRunner(Executors.newSingleThreadExecutor());
    update =
        new Update<String, TestEvent, TestEffect>() {
          @Nonnull
          @Override
          public com.spotify.mobius2.Next<String, TestEffect> update(
              String model, TestEvent mobiusEvent) {

            if (mobiusEvent instanceof EventWithCrashingEffect) {
              return com.spotify.mobius2.Next.next("will crash", effects(new Crash()));
            } else if (mobiusEvent instanceof EventWithSafeEffect) {
              EventWithSafeEffect event = (EventWithSafeEffect) mobiusEvent;
              return com.spotify.mobius2.Next.next(
                  model + "->" + mobiusEvent.toString(), effects(new SafeEffect(event.toString())));
            } else {
              return Next.next(model + "->" + mobiusEvent.toString());
            }
          }
        };

    startModel = "init";
    startEffects = ImmutableUtil.emptySet();

    effectHandler =
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                if (effectObserver != null) {
                  effectObserver.accept(effect);
                }
                if (effect instanceof Crash) {
                  throw new RuntimeException("Crashing!");
                }
              }
            };

    setupWithEffects(effectHandler, immediateRunner);
  }

  @After
  public void tearDown() throws Exception {
    backgroundRunner.dispose();
  }

  protected void setupWithEffects(
      com.spotify.mobius2.Connectable<TestEffect, TestEvent> effectHandler,
      WorkRunner effectRunner) {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(
            update,
            startModel,
            startEffects,
            effectHandler,
            eventSource,
            immediateRunner,
            effectRunner);

    mobiusLoop.observe(observer);
  }

  static class FakeEffectHandler implements Connectable<TestEffect, TestEvent> {

    private volatile Consumer<TestEvent> eventConsumer = null;

    void emitEvent(TestEvent event) {
      // throws NPE if not connected; that's OK
      eventConsumer.accept(event);
    }

    @Nonnull
    @Override
    public com.spotify.mobius2.Connection<TestEffect> connect(Consumer<TestEvent> output)
        throws com.spotify.mobius2.ConnectionLimitExceededException {
      if (eventConsumer != null) {
        throw new ConnectionLimitExceededException();
      }

      eventConsumer = output;

      return new Connection<TestEffect>() {
        @Override
        public void accept(TestEffect value) {
          // do nothing
        }

        @Override
        public void dispose() {
          // do nothing
        }
      };
    }
  }
}
