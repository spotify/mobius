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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import rx.functions.Func1;
import rx.observers.AssertableSubscriber;
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
    Func1<String, Integer> function = s -> s.length();
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
    Func1<String, Integer> function =
        s -> {
          throw new RuntimeException("Something bad happened");
        };
    AssertableSubscriber<Integer> observer =
        upstream.compose(Transformers.fromFunction(function, scheduler)).test();

    upstream.onNext("Hello");
    scheduler.triggerActions();
    observer.assertError(RuntimeException.class);
  }
}
