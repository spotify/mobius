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
package com.spotify.mobius2;

import static com.spotify.mobius2.Effects.effects;
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
    com.spotify.mobius2.First<String, String> f =
        com.spotify.mobius2.First.first("hi", effects("effect", "äffäkt"));

    assertThat(f.model(), is("hi"));
    assertTrue(f.hasEffects());
    assertThat(f.effects(), hasItems("effect", "äffäkt"));
  }

  @Test
  public void supportsCreatingWithSet() throws Exception {
    com.spotify.mobius2.First<String, String> f =
        com.spotify.mobius2.First.first("hi", ImmutableSet.of("ho", "there"));

    assertTrue(f.hasEffects());
    assertThat(f.effects(), contains("ho", "there"));
  }

  @Test
  public void reportsNoEffectsIfThereAreNoEffects() throws Exception {
    com.spotify.mobius2.First<String, String> f = com.spotify.mobius2.First.first("hi");

    assertFalse(f.hasEffects());
  }

  @Test
  public void shouldHaveCorrectEqualsWithEffects() throws Exception {
    com.spotify.mobius2.First<String, String> av1 =
        new AutoValue_First<>("hi", ImmutableSet.of("hello", "there"));
    com.spotify.mobius2.First<String, String> f1 =
        com.spotify.mobius2.First.first("hi", effects("hello", "there"));
    com.spotify.mobius2.First<String, String> f2 =
        com.spotify.mobius2.First.first("hi", effects("there", "hello"));
    com.spotify.mobius2.First<String, String> f3 =
        com.spotify.mobius2.First.first("hi", ImmutableSet.of("hello", "there"));

    com.spotify.mobius2.First<String, String> av2 =
        new AutoValue_First<>("hi", ImmutableSet.of("hello", "there", "you"));
    com.spotify.mobius2.First<String, String> g1 =
        com.spotify.mobius2.First.first("hi", effects("hello", "there", "you"));
    com.spotify.mobius2.First<String, String> g2 =
        com.spotify.mobius2.First.first("hi", ImmutableSet.of("hello", "there", "you"));

    com.spotify.mobius2.First<String, String> av3 =
        new AutoValue_First<>("hi", ImmutableSet.<String>of());
    com.spotify.mobius2.First<String, String> h1 = com.spotify.mobius2.First.first("hi");
    com.spotify.mobius2.First<String, String> h2 =
        com.spotify.mobius2.First.first("hi", ImmutableSet.<String>of());
    com.spotify.mobius2.First<String, String> h3 = com.spotify.mobius2.First.first("hi", effects());

    com.spotify.mobius2.First<String, String> i1 =
        com.spotify.mobius2.First.first("hey", effects("hello", "there"));
    com.spotify.mobius2.First<String, String> j1 =
        com.spotify.mobius2.First.first("hey", effects("hello", "there", "you"));
    com.spotify.mobius2.First<String, String> k1 = com.spotify.mobius2.First.first("hey");
    com.spotify.mobius2.First<String, String> k2 =
        com.spotify.mobius2.First.first("hey", effects());

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
  private com.spotify.mobius2.First<?, Number> canInferFromVarargOnlyEffects() {
    return com.spotify.mobius2.First.first("s", effects((short) 1, 2, (long) 3));
  }

  // Should compile
  @SuppressWarnings("unused")
  private com.spotify.mobius2.First<?, Number> canInferFromVarargOnlyEffectsSingle() {
    return First.first("s", effects((short) 1));
  }
}
