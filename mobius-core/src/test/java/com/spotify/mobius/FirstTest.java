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
package com.spotify.mobius;

import static com.spotify.mobius.Effects.effects;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class FirstTest {

  @Test
  public void supportsCreatingWithVarargs() throws Exception {
    First<String, String> f = First.first("hi", effects("effect", "äffäkt"));

    assertThat(f.model(), is("hi"));
    assertTrue(f.hasEffects());
    assertThat(f.effects(), hasItems("effect", "äffäkt"));
  }

  @Test
  public void supportsCreatingWithSet() throws Exception {
    First<String, String> f = First.first("hi", ImmutableSet.of("ho", "there"));

    assertTrue(f.hasEffects());
    assertThat(f.effects(), contains("ho", "there"));
  }

  @Test
  public void reportsNoEffectsIfThereAreNoEffects() throws Exception {
    First<String, String> f = First.first("hi");

    assertFalse(f.hasEffects());
  }

  @Test
  public void shouldHaveCorrectEqualsWithEffects() throws Exception {
    First<String, String> av1 = new AutoValue_First<>("hi", ImmutableSet.of("hello", "there"));
    First<String, String> f1 = First.first("hi", effects("hello", "there"));
    First<String, String> f2 = First.first("hi", effects("there", "hello"));
    First<String, String> f3 = First.first("hi", ImmutableSet.of("hello", "there"));

    First<String, String> av2 =
        new AutoValue_First<>("hi", ImmutableSet.of("hello", "there", "you"));
    First<String, String> g1 = First.first("hi", effects("hello", "there", "you"));
    First<String, String> g2 = First.first("hi", ImmutableSet.of("hello", "there", "you"));

    First<String, String> av3 = new AutoValue_First<>("hi", ImmutableSet.<String>of());
    First<String, String> h1 = First.first("hi");
    First<String, String> h2 = First.first("hi", ImmutableSet.<String>of());
    First<String, String> h3 = First.first("hi", effects());

    First<String, String> i1 = First.first("hey", effects("hello", "there"));
    First<String, String> j1 = First.first("hey", effects("hello", "there", "you"));
    First<String, String> k1 = First.first("hey");
    First<String, String> k2 = First.first("hey", effects());

    new EqualsTester()
        .addEqualityGroup(av1, f1, f2, f3)
        .addEqualityGroup(av2, g1, g2)
        .addEqualityGroup(av3, h1, h2, h3)
        .addEqualityGroup(i1)
        .addEqualityGroup(j1)
        .addEqualityGroup(k1, k2)
        .testEquals();
  }

  // NOTE: the below code doesn't compile with Java 7, but works with Java 8+. This means Java 7
  // users (who don't use DataEnum) will need to provide explicit type parameters, or use
  // intermediate variables for their effect sets.

  // Should compile
  @SuppressWarnings("unused")
  private First<?, Number> canInferFromVarargOnlyEffects() {
    return First.first("s", effects((short) 1, 2, (long) 3));
  }

  // Should compile
  @SuppressWarnings("unused")
  private First<?, Number> canInferFromVarargOnlyEffectsSingle() {
    return First.first("s", effects((short) 1));
  }
}
