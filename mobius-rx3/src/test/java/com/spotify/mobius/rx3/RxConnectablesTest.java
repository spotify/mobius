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

import static org.assertj.core.api.Assertions.assertThat;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

/** RxConnectablesTest. */
public class RxConnectablesTest {
  private PublishSubject<String> input;

  private Connectable<String, Integer> connectable;

  @Before
  public void setUp() throws Exception {
    input = PublishSubject.create();
    connectable =
        new Connectable<String, Integer>() {
          @Nonnull
          @Override
          public Connection<String> connect(final Consumer<Integer> output)
              throws ConnectionLimitExceededException {
            return new Connection<String>() {
              @Override
              public void accept(String value) {
                if (value.equals("crash")) {
                  throw new RuntimeException("crashing!");
                }
                output.accept(value.length());
              }

              @Override
              public void dispose() {}
            };
          }
        };
  }

  @Test
  public void shouldPropagateCompletion() throws Exception {
    TestObserver<Integer> observer =
        input.compose(RxConnectables.toTransformer(connectable)).test();

    input.onNext("hi");
    input.onComplete();

    observer.awaitDone(1, TimeUnit.SECONDS);
    observer.assertComplete();
  }

  @Test
  public void shouldPropagateErrorsFromConnectable() throws Exception {
    TestObserver<Integer> observer =
        input.compose(RxConnectables.toTransformer(connectable)).test();

    input.onNext("crash");

    observer.awaitDone(1, TimeUnit.SECONDS);
    observer.assertError(throwable -> throwable.getMessage().equals("crashing!"));
  }

  @Test
  public void shouldPropagateErrorsFromUpstream() throws Exception {
    final Throwable expected = new RuntimeException("expected");

    TestObserver<Integer> observer =
        input.compose(RxConnectables.toTransformer(connectable)).test();

    input.onError(expected);

    observer.awaitDone(1, TimeUnit.SECONDS);
    observer.assertError(expected);
  }

  @Test
  public void fromTransformerForwarding() {
    final RecordingConsumer<Integer> consumer = new RecordingConsumer<>();
    RxConnectables.fromTransformer(upstream -> Observable.just(1)).connect(consumer);
    consumer.assertValues(1);
  }

  @Test
  public void toTransformerNoEventsAreGeneratedAfterDispose()
      throws InterruptedException, ExecutionException {
    Connectable<Integer, String> intToString =
        new Connectable<>() {
          @Nonnull
          @Override
          public Connection<Integer> connect(Consumer<String> output)
              throws ConnectionLimitExceededException {
            return new Connection<Integer>() {
              @Override
              public void accept(Integer value) {
                output.accept(value.toString());
              }

              @Override
              public void dispose() {}
            };
          }
        };

    // given an observable that continuously emits stuff
    ExecutorService executor = Executors.newSingleThreadExecutor();
    PublishSubject<Integer> subject = PublishSubject.create();

    final AtomicInteger count = new AtomicInteger();
    final AtomicBoolean stop = new AtomicBoolean();

    Future<?> future =
        executor.submit(
            () -> {
              while (!stop.get()) {
                subject.onNext(count.incrementAndGet());
              }
            });

    // and a connectable to that observable
    ObservableTransformer<Integer, String> transformer = RxConnectables.toTransformer(intToString);
    Observable<String> underTest = subject.compose(transformer);

    // when a connectable is subscribed to (many times to make this non-flaky/less flaky)
    for (int i = 1; i < 1000; i++) {

      // then, the event observer doesn't receive events after it has been disposed.
      EventRxConsumer consumer = new EventRxConsumer();

      Disposable disposable = underTest.subscribe(consumer);

      // the sleep here and below is not strictly necessary, but it helps provoke errors more
      // frequently (on my laptop at least..). YMMV in case there is another issue like this one
      // in the future.
      Thread.sleep(1);

      disposable.dispose();
      consumer.disposed = true;

      Thread.sleep(3);

      assertThat(consumer.acceptCalledAfterDispose)
          .describedAs("accept called after dispose on attempt %d", i)
          .isFalse();
    }

    stop.set(true);
    future.get();
  }

  @Test
  public void fromTransformerNoEventsAreGeneratedAfterDispose()
      throws InterruptedException, ExecutionException {
    ObservableTransformer<Integer, String> intToString =
        new ObservableTransformer<Integer, String>() {
          @Override
          public @NonNull ObservableSource<String> apply(@NonNull Observable<Integer> upstream) {
            return upstream.map(Object::toString);
          }
        };

    // given an observable that continuously emits stuff
    ExecutorService executor = Executors.newSingleThreadExecutor();
    PublishSubject<Integer> subject = PublishSubject.create();

    final AtomicInteger count = new AtomicInteger();
    final AtomicBoolean stop = new AtomicBoolean();

    Future<?> future =
        executor.submit(
            () -> {
              while (!stop.get()) {
                subject.onNext(count.incrementAndGet());
              }
            });

    // and a connectable to that observable
    Connectable<Integer, String> underTest = RxConnectables.fromTransformer(intToString);

    // when a connectable is subscribed to (many times to make this non-flaky/less flaky)
    for (int i = 1; i < 1000; i++) {

      // then, the event observer doesn't receive events after it has been disposed.
      EventConsumer consumer = new EventConsumer();
      Connection<Integer> input = underTest.connect(consumer);

      Disposable disposable = subject.subscribe(input::accept);

      // the sleep here and below is not strictly necessary, but it helps provoke errors more
      // frequently (on my laptop at least..). YMMV in case there is another issue like this one
      // in the future.
      Thread.sleep(1);

      input.dispose();
      disposable.dispose();
      consumer.disposed = true;

      Thread.sleep(3);

      assertThat(consumer.acceptCalledAfterDispose)
          .describedAs("accept called after dispose on attempt %d", i)
          .isFalse();
    }

    stop.set(true);
    future.get();
  }

  private static class EventRxConsumer implements io.reactivex.rxjava3.functions.Consumer<String> {
    public volatile boolean disposed = false;
    public volatile boolean acceptCalledAfterDispose = false;

    @Override
    public void accept(String s) throws Throwable {
      if (disposed) {
        acceptCalledAfterDispose = true;
      }
    }
  }

  private static class EventConsumer implements Consumer<String> {
    public volatile boolean disposed = false;
    public volatile boolean acceptCalledAfterDispose = false;

    @Override
    public void accept(String value) {
      if (disposed) {
        acceptCalledAfterDispose = true;
      }
    }
  }
}
