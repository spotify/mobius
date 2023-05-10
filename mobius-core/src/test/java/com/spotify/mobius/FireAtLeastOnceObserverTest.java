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
package com.spotify.mobius;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class FireAtLeastOnceObserverTest {
  List<Integer> observed;

  FireAtLeastOnceObserver<Integer> observer;

  @Before
  public void setUp() throws Exception {
    observed = new ArrayList<>();

    observer = new FireAtLeastOnceObserver<>(observed::add);
  }

  @Test
  public void shouldForwardAcceptValuesNormally() {
    observer.accept(1);
    observer.accept(875);

    assertThat(observed).containsExactly(1, 875);
  }

  @Test
  public void shouldForwardAcceptFirstOnce() {
    observer.acceptIfFirst(98);

    assertThat(observed).containsExactly(98);
  }

  @Test
  public void shouldForwardAcceptNormallyAfterAcceptFirst() {
    observer.acceptIfFirst(87);
    observer.accept(87678);

    assertThat(observed).containsExactly(87, 87678);
  }

  @Test
  public void shouldNotForwardAcceptFirstTwice() {
    observer.acceptIfFirst(87);
    observer.acceptIfFirst(7767);

    assertThat(observed).containsExactly(87);
  }

  @Test
  public void shouldNotForwardAcceptFirstAfterNormalAccept() {
    observer.acceptIfFirst(987987);
    observer.acceptIfFirst(7767);

    assertThat(observed).containsExactly(987987);
  }
}
