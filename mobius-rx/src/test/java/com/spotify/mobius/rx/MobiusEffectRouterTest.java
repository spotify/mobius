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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.auto.value.AutoValue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class MobiusEffectRouterTest {

  private TestSubscriber<TestEvent> testSubscriber;
  private PublishSubject<TestEffect> publishSubject;
  private TestConsumer<C> cConsumer;
  private TestAction dAction;

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    cConsumer = new TestConsumer<>();
    dAction = new TestAction();
    Transformer<TestEffect, TestEvent> router =
        RxMobius.<TestEffect, TestEvent>subtypeEffectHandler()
            .addTransformer(A.class, (Observable<A> as) -> as.map(a -> AEvent.create(a.id())))
            .addTransformer(B.class, (Observable<B> bs) -> bs.map(b -> BEvent.create(b.id())))
            .addConsumer(C.class, cConsumer)
            .addAction(D.class, dAction)
            .addFunction(E.class, e -> AEvent.create(e.id()))
            .build();

    publishSubject = PublishSubject.create();
    testSubscriber = TestSubscriber.create();

    publishSubject.compose(router).subscribe(testSubscriber);
  }

  @Test
  public void shouldRouteEffectToPerformer() throws Exception {
    publishSubject.onNext(A.create(456));
    publishSubject.onCompleted();

    testSubscriber.awaitTerminalEvent();
    testSubscriber.assertValue(AEvent.create(456));
  }

  @Test
  public void shouldRouteEffectToConsumer() throws Exception {
    publishSubject.onNext(C.create(456));
    publishSubject.onCompleted();

    testSubscriber.awaitTerminalEvent();
    assertThat(cConsumer.getCurrentValue(), is(equalTo(C.create(456))));
  }

  @Test
  public void shouldRunActionOnConsumer() throws Exception {
    publishSubject.onNext(D.create(123));
    publishSubject.onNext(D.create(456));
    publishSubject.onNext(D.create(789));
    publishSubject.onCompleted();

    testSubscriber.awaitTerminalEvent();
    assertThat(dAction.getRunCount(), is(equalTo(3)));
  }

  @Test
  public void shouldInvokeFunctionAndEmitEvent() {
    publishSubject.onNext(E.create(123));
    publishSubject.onCompleted();

    testSubscriber.awaitTerminalEvent();
    testSubscriber.assertValue(AEvent.create(123));
  }

  @Test
  public void shouldFailForUnhandledEffect() throws Exception {
    Unhandled unhandled = Unhandled.create();
    publishSubject.onNext(unhandled);

    testSubscriber.awaitTerminalEvent();
    testSubscriber.assertError(new UnknownEffectException(unhandled));
  }

  @Test
  public void shouldReportEffectClassCollisionWhenAddingSuperclass() throws Exception {
    RxMobius.SubtypeEffectHandlerBuilder<TestEffect, TestEvent> builder =
        RxMobius.<TestEffect, TestEvent>subtypeEffectHandler()
            .addTransformer(Child.class, childObservable -> null);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("collision");

    builder.addTransformer(Parent.class, parentObservable -> null);
  }

  @Test
  public void shouldReportEffectClassCollisionWhenAddingSubclass() throws Exception {
    RxMobius.SubtypeEffectHandlerBuilder<TestEffect, TestEvent> builder =
        RxMobius.<TestEffect, TestEvent>subtypeEffectHandler()
            .addTransformer(Parent.class, observable -> null);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("collision");

    builder.addTransformer(Child.class, observable -> null);
  }

  @Test
  public void effectHandlersShouldBeImmutable() throws Exception {
    // redo some test setup for test case specific conditions
    publishSubject = PublishSubject.create();
    testSubscriber = TestSubscriber.create();

    RxMobius.SubtypeEffectHandlerBuilder<TestEffect, TestEvent> builder =
        RxMobius.<TestEffect, TestEvent>subtypeEffectHandler()
            .addTransformer(A.class, (Observable<A> as) -> as.map(a -> AEvent.create(a.id())));

    Transformer<TestEffect, TestEvent> router = builder.build();

    // this should not lead to the effects router being capable of handling B effects
    builder.addTransformer(B.class, (Observable<B> bs) -> bs.map(b -> BEvent.create(b.id())));

    publishSubject.compose(router).subscribe(testSubscriber);

    B effect = B.create(84);
    publishSubject.onNext(effect);
    publishSubject.onCompleted();

    testSubscriber.awaitTerminalEvent(3, TimeUnit.SECONDS);
    testSubscriber.assertError(new UnknownEffectException(effect));
  }

  @Test
  public void shouldSupportCustomErrorHandler() throws Exception {
    // redo some test setup for test case specific conditions
    publishSubject = PublishSubject.create();
    testSubscriber = TestSubscriber.create();

    final RuntimeException expectedException = new RuntimeException("expected!");
    final AtomicBoolean gotRightException = new AtomicBoolean(false);

    RxMobius.SubtypeEffectHandlerBuilder<TestEffect, TestEvent> builder =
        RxMobius.<TestEffect, TestEvent>subtypeEffectHandler()
            .addFunction(
                A.class,
                a -> {
                  throw expectedException;
                })
            .withFatalErrorHandler(
                new Func1<Transformer<? extends TestEffect, TestEvent>, Action1<Throwable>>() {
                  @Override
                  public Action1<Throwable> call(
                      Transformer<? extends TestEffect, TestEvent> testEventTransformer) {
                    return new Action1<Throwable>() {
                      @Override
                      public void call(Throwable throwable) {
                        if (throwable.equals(expectedException)) {
                          gotRightException.set(true);
                        } else {
                          throwable.printStackTrace();
                          Assert.fail("got the wrong exception!");
                        }
                      }
                    };
                  }
                });

    Transformer<TestEffect, TestEvent> router = builder.build();

    publishSubject.compose(router).subscribe(testSubscriber);

    publishSubject.onNext(A.create(1));

    assertThat(gotRightException.get(), is(true));

    testSubscriber.awaitTerminalEvent(3, TimeUnit.SECONDS);
    testSubscriber.assertError(expectedException);
  }

  private interface TestEffect {}

  @AutoValue
  abstract static class A implements TestEffect {

    abstract int id();

    static A create(int id) {
      return new AutoValue_MobiusEffectRouterTest_A(id);
    }
  }

  @AutoValue
  public abstract static class B implements TestEffect {

    abstract int id();

    static B create(int id) {
      return new AutoValue_MobiusEffectRouterTest_B(id);
    }
  }

  @AutoValue
  public abstract static class C implements TestEffect {

    abstract int id();

    static C create(int id) {
      return new AutoValue_MobiusEffectRouterTest_C(id);
    }
  }

  @AutoValue
  public abstract static class D implements TestEffect {

    abstract int id();

    static D create(int id) {
      return new AutoValue_MobiusEffectRouterTest_D(id);
    }
  }

  @AutoValue
  public abstract static class E implements TestEffect {

    abstract int id();

    static E create(int id) {
      return new AutoValue_MobiusEffectRouterTest_E(id);
    }
  }

  @AutoValue
  public abstract static class Unhandled implements TestEffect {

    static Unhandled create() {
      return new AutoValue_MobiusEffectRouterTest_Unhandled();
    }
  }

  private static class Parent implements TestEffect {}

  private static class Child extends Parent {}

  private interface TestEvent {}

  @AutoValue
  public abstract static class AEvent implements TestEvent {

    abstract int id();

    static AEvent create(int id) {
      return new AutoValue_MobiusEffectRouterTest_AEvent(id);
    }
  }

  @AutoValue
  public abstract static class BEvent implements TestEvent {

    abstract int id();

    static BEvent create(int id) {
      return new AutoValue_MobiusEffectRouterTest_BEvent(id);
    }
  }
}
