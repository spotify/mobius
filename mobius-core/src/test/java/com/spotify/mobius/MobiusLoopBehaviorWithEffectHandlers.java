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

import static com.spotify.mobius.Effects.effects;
import static org.awaitility.Awaitility.await;

import com.google.common.util.concurrent.SettableFuture;
import com.spotify.mobius.runners.ExecutorServiceWorkRunner;
import com.spotify.mobius.test.SimpleConnection;
import com.spotify.mobius.test.TestWorkRunner;
import com.spotify.mobius.testdomain.EventWithCrashingEffect;
import com.spotify.mobius.testdomain.EventWithSafeEffect;
import com.spotify.mobius.testdomain.SafeEffect;
import com.spotify.mobius.testdomain.TestEffect;
import com.spotify.mobius.testdomain.TestEvent;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusLoopBehaviorWithEffectHandlers extends MobiusLoopTest {
  @Test
  public void shouldSurviveEffectPerformerThrowing() throws Exception {
    mobiusLoop.dispatchEvent(new EventWithCrashingEffect());
    mobiusLoop.dispatchEvent(new TestEvent("should happen"));

    observer.assertStates("init", "will crash", "will crash->should happen");
  }

  @Test
  public void shouldSurviveEffectPerformerThrowingMultipleTimes() throws Exception {
    mobiusLoop.dispatchEvent(new EventWithCrashingEffect());
    mobiusLoop.dispatchEvent(new TestEvent("should happen"));
    mobiusLoop.dispatchEvent(new EventWithCrashingEffect());
    mobiusLoop.dispatchEvent(new TestEvent("should happen, too"));

    observer.assertStates(
        "init",
        "will crash",
        "will crash->should happen",
        "will crash",
        "will crash->should happen, too");
  }

  @Test
  public void shouldSupportEffectsThatGenerateEvents() throws Exception {
    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                eventConsumer.accept(new TestEvent(effect.toString()));
              }
            },
        immediateRunner);

    mobiusLoop.dispatchEvent(new EventWithSafeEffect("hi"));

    observer.assertStates("init", "init->hi", "init->hi->effecthi");
  }

  @Test
  public void shouldOrderStateChangesCorrectlyWhenEffectsAreSlow() throws Exception {
    final SettableFuture<TestEvent> future = SettableFuture.create();

    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                try {
                  eventConsumer.accept(future.get());

                } catch (InterruptedException | ExecutionException e) {
                  e.printStackTrace();
                }
              }
            },
        backgroundRunner);

    mobiusLoop.dispatchEvent(new EventWithSafeEffect("1"));
    mobiusLoop.dispatchEvent(new TestEvent("2"));

    await().atMost(Duration.ofSeconds(1)).until(() -> observer.valueCount() >= 3);

    future.set(new TestEvent("3"));

    await().atMost(Duration.ofSeconds(1)).until(() -> observer.valueCount() >= 4);
    observer.assertStates("init", "init->1", "init->1->2", "init->1->2->3");
  }

  @Test
  public void shouldSupportHandlingEffectsWhenOneEffectNeverCompletes() throws Exception {
    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                if (effect instanceof SafeEffect) {
                  if (((SafeEffect) effect).id.equals("1")) {
                    try {
                      // Rough approximation of waiting infinite amount of time.
                      Thread.sleep(2000);
                    } catch (InterruptedException e) {
                      // ignored.
                    }
                    return;
                  }
                }

                eventConsumer.accept(new TestEvent(effect.toString()));
              }
            },
        new ExecutorServiceWorkRunner(Executors.newFixedThreadPool(2)));

    // the effectHandler associated with "1" should never happen
    mobiusLoop.dispatchEvent(new EventWithSafeEffect("1"));
    mobiusLoop.dispatchEvent(new TestEvent("2"));
    mobiusLoop.dispatchEvent(new EventWithSafeEffect("3"));

    await().atMost(Duration.ofSeconds(5)).until(() -> observer.valueCount() >= 5);

    observer.assertStates(
        "init", "init->1", "init->1->2", "init->1->2->3", "init->1->2->3->effect3");
  }

  @Test
  public void shouldPerformEffectFromInit() throws Exception {

    update =
        new Update<String, TestEvent, TestEffect>() {
          @Nonnull
          @Override
          public Next<String, TestEffect> update(String model, TestEvent event) {
            return Next.next(model + "->" + event.toString());
          }
        };

    startModel = "init";
    startEffects = effects(new SafeEffect("frominit"));

    TestWorkRunner testWorkRunner = new TestWorkRunner();

    setupWithEffects(
        eventConsumer ->
            new SimpleConnection<TestEffect>() {
              @Override
              public void accept(TestEffect effect) {
                eventConsumer.accept(new TestEvent(effect.toString()));
              }
            },
        testWorkRunner);

    observer.waitForChange(100);
    testWorkRunner.runAll();

    observer.assertStates("init", "init->effectfrominit");
  }
}
