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

import static com.spotify.mobius.Effects.effects;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Set;
import org.junit.Test;

public class EffectsTest {

  @Test
  public void returnsEmptyMutableSetWhenNoEffectsProvided() {
    Set<String> result = effects();

    assertThat(result, is(empty()));

    result.add("first");
    assertThat(result, contains("first"));
  }

  @Test
  public void returnsSetWithSpecifiedEffects() {
    Set<String> result = effects("a", "b", "c");

    assertThat(result, containsInAnyOrder("a", "b", "c"));
  }

  @Test
  public void deduplicatesIdenticalEffects() {
    Set<String> result = effects("a", "b", "a");

    assertThat(result, containsInAnyOrder("a", "b"));
    assertThat(result.size(), is(2));
  }

  @Test
  public void supportsSubtypesOfEffectSuperType() {
    Integer i = 1;
    Double d = 2.0;
    Set<Number> result = effects(i, d);

    assertThat(result, containsInAnyOrder((Number) 1, (Number) 2.0));
  }

  @Test(expected = NullPointerException.class)
  public void throwsNpeWhenArrayIsNull() {
    effects((Object[]) null);
  }

  @Test(expected = NullPointerException.class)
  public void throwsNpeWhenElementIsNull() {
    effects("valid", null);
  }
}
