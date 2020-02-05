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
package com.spotify.mobius.rx;

import static com.google.common.collect.Lists.transform;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.spotify.mobius.functions.Function;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class TransformersTest {

  private PublishSubject<String> upstream;
  private TestAction action;
  private TestScheduler scheduler;
  private TestConsumer<String> consumer;

  @Before
  public void setUp() throws Exception {
    upstream = PublishSubject.create();
    action = new TestAction();
    scheduler = new TestScheduler();
    consumer = new TestConsumer<>();
  }

  @Test
  public void effectPerformerRunsActionWheneverEffectIsRequested() throws Exception {
    upstream.compose(Transformers.fromAction(action)).subscribe();
    upstream.onNext("First Time");
    assertThat(action.getRunCount(), is(1));

    upstream.onNext("One more!");
    assertThat(action.getRunCount(), is(2));
  }

  @Test
  public void effectPerformerRunsActionOnSchedulerWheneverEffectIsRequested() throws Exception {
    upstream.compose(Transformers.fromAction(action, scheduler)).subscribe();

    upstream.onNext("First Time");
    assertThat(action.getRunCount(), is(0));
    scheduler.triggerActions();
    assertThat(action.getRunCount(), is(1));
  }

  @Test
  public void effectPerformerInvokesConsumerAndPassesTheRequestedEffect() throws Exception {
    upstream.compose(Transformers.fromConsumer(consumer)).subscribe();

    upstream.onNext("First Time");
    assertThat(consumer.getCurrentValue(), is("First Time"));

    upstream.onNext("Do it again!");
    assertThat(consumer.getCurrentValue(), is("Do it again!"));
  }

  @Test
  public void effectPerformerInvokesConsumerOnSchedulerAndPassesTheRequestedEffect()
      throws Exception {
    upstream.compose(Transformers.fromConsumer(consumer, scheduler)).subscribe();

    upstream.onNext("First Time");
    assertThat(consumer.getCurrentValue(), is(equalTo(null)));
    scheduler.triggerActions();
    assertThat(consumer.getCurrentValue(), is("First Time"));
  }

  @Test
  public void consumerTransformerShouldPropagateCompletion() throws Exception {
    AssertableSubscriber<Object> subscriber =
        upstream.compose(Transformers.fromConsumer(consumer, scheduler)).test();

    upstream.onNext("hi");
    upstream.onCompleted();

    scheduler.triggerActions();

    subscriber.awaitTerminalEvent(1, TimeUnit.SECONDS);
    subscriber.assertCompleted();
  }

  @Test
  public void effectPerformerInvokesFunctionWithReceivedEffectAndEmitsReturnedEvents() {
    PublishSubject<String> upstream = PublishSubject.create();
    TestScheduler scheduler = new TestScheduler();
    Function<String, Integer> function = s -> s.length();
    AssertableSubscriber<Integer> observer =
        upstream.compose(Transformers.fromFunction(function, scheduler)).test();

    upstream.onNext("Hello");
    scheduler.triggerActions();
    observer.assertValue(5);
  }

  @Test
  public void effectPerformerInvokesFunctionWithReceivedEffectAndErrorsForUnhandledExceptions() {
    PublishSubject<String> upstream = PublishSubject.create();
    TestScheduler scheduler = new TestScheduler();
    Function<String, Integer> function =
        s -> {
          throw new RuntimeException("Something bad happened");
        };
    AssertableSubscriber<Integer> observer =
        upstream.compose(Transformers.fromFunction(function, scheduler)).test();

    upstream.onNext("Hello");
    scheduler.triggerActions();
    observer.assertError(RuntimeException.class);
  }

  @Test
  public void processingLongEffectsDoesNotBlockProcessingShorterEffects() {
    final List<String> effects = Arrays.asList("Hello", "Rx1");

    PublishSubject<String> upstream = PublishSubject.create();
    Function<String, Integer> sleepyFunction =
        s -> {
          try {
            Thread.sleep(duration(s));
          } catch (InterruptedException ie) {
          }
          return s.length();
        };

    final List<Integer> results = new ArrayList<>();
    upstream
        .compose(Transformers.fromFunction(sleepyFunction, Schedulers.io()))
        .subscribe(results::add);

    Observable.from(effects).subscribe(upstream);

    await().atMost(durationForEffects(effects)).until(() -> results.equals(expected(effects)));
  }

  private Duration durationForEffects(List<String> effects) {
    int maxDuration = -1;
    for (String f : effects) {
      if (duration(f) > maxDuration) maxDuration = duration(f);
    }
    // Since effects are processed in parallel thanks to FlatMap
    // we only wait the max time and add 100 milliseconds to
    // avoid test flakiness thanks to time
    return Duration.ofMillis(maxDuration + 100);
  }

  private int duration(String f) {
    return f.length() * 100;
  }

  private List<Integer> expected(List<String> effects) {
    List<Integer> lengths = new ArrayList<>(transform(effects, s -> s == null ? 0 : s.length()));
    lengths.sort(Integer::compare);
    return lengths;
  }
}
