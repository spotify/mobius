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

import static org.assertj.core.api.Assertions.assertThat;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import rx.Observable;
import rx.subjects.PublishSubject;

public class RxEventSourcesTest {

  @Rule public final RxErrorsRule rxErrorsRule = new RxErrorsRule();

  @Test
  public void eventsAreForwardedInOrder() throws Exception {
    EventSource<Integer> source = RxEventSources.fromObservables(Observable.just(1, 2, 3));
    RecordingConsumer<Integer> consumer = new RecordingConsumer<>();

    source.subscribe(consumer);

    consumer.waitForChange(50);
    consumer.assertValues(1, 2, 3);
  }

  @Test
  public void disposePreventsFurtherEvents() throws Exception {
    PublishSubject<Integer> subject = PublishSubject.create();
    EventSource<Integer> source = RxEventSources.fromObservables(subject);
    RecordingConsumer<Integer> consumer = new RecordingConsumer<>();

    Disposable d = source.subscribe(consumer);

    subject.onNext(1);
    subject.onNext(2);
    d.dispose();
    subject.onNext(3);

    consumer.waitForChange(50);
    consumer.assertValues(1, 2);
  }

  @Test
  public void errorsAreForwardedToErrorHandler() throws Exception {
    PublishSubject<Integer> subject = PublishSubject.create();
    final EventSource<Integer> source = RxEventSources.fromObservables(subject);
    RecordingConsumer<Integer> consumer = new RecordingConsumer<>();

    source.subscribe(consumer);
    subject.onError(new RuntimeException("crash!"));

    rxErrorsRule.assertSingleErrorWithMessage("crash!");
  }

  @Test
  public void noEventsAreGeneratedAfterDispose() throws InterruptedException, ExecutionException {
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

    // and an event source based on that observable
    EventSource<Integer> source = RxEventSources.fromObservables(subject);

    // when an event source is subscribed to (many times to make this non-flaky/less flaky)
    // NOTE: this value is currently set too low for reliable detection - the idea is that there
    // used to be a bug here, which has now been fixed, so there's no point in running these tests
    // for too long and slowing down all builds. But running them 'a bit' should hopefully lead to
    // at least some occasional flakiness, meaning that we'll be able to detect errors sooner or
    // later, without paying too much for slow tests.
    for (int i = 1; i < 100; i++) {

      // then, the event consumer doesn't receive events after it has been disposed.
      EventConsumer consumer = new EventConsumer();

      Disposable disposable = source.subscribe(consumer);

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

  private static class EventConsumer implements Consumer<Integer> {
    public volatile boolean disposed = false;
    public volatile boolean acceptCalledAfterDispose = false;

    @Override
    public void accept(Integer value) {
      if (disposed) {
        acceptCalledAfterDispose = true;
      }
    }
  }
}
