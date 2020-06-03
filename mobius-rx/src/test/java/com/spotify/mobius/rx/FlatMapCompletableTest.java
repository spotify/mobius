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
package com.spotify.mobius.rx;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class FlatMapCompletableTest {

  @Test
  public void invokesTheActionThenCompletesAndEmitsNothing() throws Exception {
    PublishSubject<Integer> values = PublishSubject.create();

    SideEffectPerformer sideEffectPerformer = new SideEffectPerformer();
    FlatMapCompletable<Integer, SomeOtherType> flatMapCompletable =
        FlatMapCompletable.createForAction(sideEffectPerformer);

    Observable<SomeOtherType> observable = values.compose(flatMapCompletable);

    TestSubscriber<SomeOtherType> subscriber = new TestSubscriber<>();
    observable.subscribe(subscriber);

    values.onNext(1);
    values.onNext(5);
    values.onNext(6);
    List<Integer> unaffectedValues = sideEffectPerformer.verifySideEffects(1, 5, 6);
    assertTrue(unaffectedValues.isEmpty());
    subscriber.assertNoValues();

    values.onCompleted();
    subscriber.assertCompleted();
  }

  @Test
  public void errorsPropagateDownTheChain() {
    PublishSubject<Integer> upstream = PublishSubject.create();
    FlatMapCompletable<Integer, SomeOtherType> flatMapCompletable =
        FlatMapCompletable.createForAction(
            new Action1<Integer>() {
              @Override
              public void call(Integer integer) {
                throw new IllegalArgumentException();
              }
            });
    Observable<SomeOtherType> observable = upstream.compose(flatMapCompletable);

    TestSubscriber<SomeOtherType> subscriber = new TestSubscriber<>();
    observable.subscribe(subscriber);

    upstream.onNext(1);
    subscriber.assertNoValues();
    subscriber.assertError(IllegalArgumentException.class);
  }

  private static class SideEffectPerformer implements Action1<Integer> {
    final List<Integer> invocations = new ArrayList<>();

    @Override
    public void call(Integer integer) {
      invocations.add(integer);
    }

    List<Integer> verifySideEffects(int... values) {
      List<Integer> unaffectedValues = new ArrayList<>();
      for (int value : values) {
        if (invocations.indexOf(value) == -1) {
          unaffectedValues.add(value);
        }
      }
      return unaffectedValues;
    }
  }

  private static class SomeOtherType {}
}
