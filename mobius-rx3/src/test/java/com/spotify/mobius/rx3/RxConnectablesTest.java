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

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.test.RecordingConsumer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.util.concurrent.TimeUnit;
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
}
