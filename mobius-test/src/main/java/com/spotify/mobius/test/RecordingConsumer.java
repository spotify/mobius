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
package com.spotify.mobius.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import com.spotify.mobius.functions.Consumer;
import java.util.Arrays;
import java.util.List;

public class RecordingConsumer<V> implements Consumer<V> {

  private final List<V> values = Lists.newArrayList();
  private final Object lock = new Object();

  @Override
  public void accept(V value) {
    synchronized (lock) {
      values.add(value);
      lock.notify();
    }
  }

  public boolean waitForChange(long timeoutMs) {
    synchronized (lock) {
      try {
        lock.wait(timeoutMs);
        return true;

      } catch (InterruptedException e) {
        return false;
      }
    }
  }

  public int valueCount() {
    synchronized (lock) {
      return values.size();
    }
  }

  @SafeVarargs
  public final void assertValues(V... expectedValues) {
    synchronized (lock) {
      assertThat(values, equalTo(Arrays.asList(expectedValues)));
    }
  }

  @SafeVarargs
  public final void assertValuesInAnyOrder(V... expectedValues) {
    synchronized (lock) {
      assertThat(values, containsInAnyOrder(expectedValues));
    }
  }
}
