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
package com.spotify.mobius.android;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.spotify.mobius.test.RecordingConsumer;
import org.junit.Test;

public class AccumulatorTest {

  private Accumulator<String> underTest = new Accumulator<>();

  @Test
  public void shouldAccumulateEvents() {
    underTest.append("1").append("2").append("3");
    RecordingConsumer<String> consumer = new RecordingConsumer<>();

    underTest.handle(consumer);

    consumer.assertValues("1", "2", "3");
  }

  @Test
  public void shouldNotSendValuesMoreThanOnce() {
    underTest.append("1").append("2");
    RecordingConsumer<String> consumer1 = new RecordingConsumer<>();
    RecordingConsumer<String> consumer2 = new RecordingConsumer<>();

    underTest.handle(consumer1);
    underTest.handle(consumer2);

    consumer1.assertValues("1", "2");
    assertThat(consumer2.valueCount(), equalTo(0));
  }

  @Test
  public void shouldNotAppendNewValuesToHandledValues() {
    underTest.append("1").append("2");
    RecordingConsumer<String> consumer1 = new RecordingConsumer<>();
    RecordingConsumer<String> consumer2 = new RecordingConsumer<>();

    underTest.handle(consumer1);
    underTest.append("3").append("4");
    underTest.handle(consumer2);

    consumer1.assertValues("1", "2");
    consumer2.assertValues("3", "4");
  }
}
