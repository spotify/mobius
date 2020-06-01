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
package com.spotify.mobius2.rx2;

import com.spotify.mobius2.EventSource;
import com.spotify.mobius2.disposables.Disposable;
import com.spotify.mobius2.test.RecordingConsumer;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.junit.Test;

public class RxEventSourcesTest {

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
}
