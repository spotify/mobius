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

import static com.google.common.collect.Lists.transform;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** TransformersTest. */
public class TransformersTest {

  @Test
  public void effectPerformerRunsActionWheneverEffectIsRequested() throws Exception {
    PublishSubject<String> upstream = PublishSubject.create();
    TestAction action = new TestAction();
    upstream.compose(Transformers.fromAction(action)).subscribe();
    upstream.onNext("First Time");
    assertThat(action.getRunCount(), is(1));

    upstream.onNext("One more!");
    assertThat(action.getRunCount(), is(2));
  }

  @Test
  public void effectPerformerRunsActionOnSchedulerWheneverEffectIsRequested() throws Exception {
    PublishSubject<String> upstream = PublishSubject.create();
    TestAction action = new TestAction();
    TestScheduler scheduler = new TestScheduler();
    upstream.compose(Transformers.fromAction(action, scheduler)).subscribe();

    upstream.onNext("First Time");
    assertThat(action.getRunCount(), is(0));
    scheduler.triggerActions();
    assertThat(action.getRunCount(), is(1));
  }

  @Test
  public void effectPerformerInvokesConsumerAndPassesTheRequestedEffect() throws Exception {
    PublishSubject<String> upstream = PublishSubject.create();
    TestConsumer<String> consumer = new TestConsumer<>();
    upstream.compose(Transformers.fromConsumer(consumer)).subscribe();

    upstream.onNext("First Time");
    assertThat(consumer.getCurrentValue(), is("First Time"));

    upstream.onNext("Do it again!");
    assertThat(consumer.getCurrentValue(), is("Do it again!"));
  }

  @Test
  public void effectPerformerInvokesConsumerOnSchedulerAndPassesTheRequestedEffect()
      throws Exception {
    PublishSubject<String> upstream = PublishSubject.create();
    TestConsumer<String> consumer = new TestConsumer<>();
    TestScheduler scheduler = new TestScheduler();
    upstream.compose(Transformers.fromConsumer(consumer, scheduler)).subscribe();

    upstream.onNext("First Time");
    assertThat(consumer.getCurrentValue(), is(equalTo(null)));
    scheduler.triggerActions();
    assertThat(consumer.getCurrentValue(), is("First Time"));
  }

  @Test
  public void effectPerformerInvokesFunctionWithReceivedEffectAndEmitsReturnedEvents() {
    PublishSubject<String> upstream = PublishSubject.create();
    TestScheduler scheduler = new TestScheduler();
    Function<String, Integer> function = String::length;
    TestObserver<Integer> observer =
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
    TestObserver<Integer> observer =
        upstream.compose(Transformers.fromFunction(function, scheduler)).test();

    upstream.onNext("Hello");
    scheduler.triggerActions();
    observer.assertError(RuntimeException.class);
  }

  @Test
  public void processingLongEffectsDoesNotBlockProcessingShorterEffects() {
    final List<String> effects = Arrays.asList("Hello", "Rx");

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
    final Disposable disposable =
        upstream
            .compose(Transformers.fromFunction(sleepyFunction, Schedulers.io()))
            .subscribe(results::add);

    Observable.fromIterable(effects).subscribe(upstream);

    await().atMost(durationForEffects(effects)).until(() -> results.equals(expected(effects)));
    disposable.dispose();
  }

  private Duration durationForEffects(List<String> effects) {
    int maxDuration = -1;
    for (String f : effects) {
      if (duration(f) > maxDuration) maxDuration = duration(f);
    }
    // Since effects are processed in parallel thanks to FlatMap
    // we only wait the max time and add some time to
    // avoid test flakiness due to time
    return Duration.ofMillis(maxDuration + 500);
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
