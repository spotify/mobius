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
import static com.spotify.mobius2.Next.dispatch;
import static com.spotify.mobius2.Next.noChange;
import static com.spotify.mobius2.internal_util.ImmutableUtil.setOf;
import static com.spotify.mobius2.internal_util.ImmutableUtil.unionSets;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import com.google.common.testing.EqualsTester;
import com.spotify.mobius2.internal_util.ImmutableUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class NextTest {

  @Test
  public void shouldNotBeSensitiveToExternalMutation() throws Exception {
    Set<String> inputs = new HashSet<>();
    inputs.add("in");

    com.spotify.mobius2.Next<String, String> next = com.spotify.mobius2.Next.next("#", inputs);

    inputs.add("don't want to see this one");

    assertThat(next.effects(), equalTo((Set) Sets.newHashSet("in")));
  }

  @Test
  public void shouldNotCareAboutEffectOrder() throws Exception {
    com.spotify.mobius2.Next<String, String> original =
        com.spotify.mobius2.Next.next("model", effects("e1", "e2"));
    com.spotify.mobius2.Next<String, String> reordered =
        com.spotify.mobius2.Next.next("model", effects("e2", "e1"));

    assertThat(reordered, equalTo(original));
  }

  @Test
  public void nextNoopHasNoModelAndNoEffects() throws Exception {
    com.spotify.mobius2.Next<String, String> next = noChange();

    assertFalse(next.hasModel());
    assertFalse(next.hasEffects());
  }

  @Test
  public void nextEffectsOnlyHasEffects() throws Exception {
    com.spotify.mobius2.Next<String, String> next = dispatch(effects("foo"));

    assertFalse(next.hasModel());
    assertTrue(next.hasEffects());
  }

  @Test
  public void nextNoEffectsOnlyHasModel() throws Exception {
    com.spotify.mobius2.Next<String, String> next = com.spotify.mobius2.Next.next("foo");

    assertTrue(next.hasModel());
    assertFalse(next.hasEffects());
  }

  @Test
  public void nextModelAndEffectsHasBothModelAndEffects() throws Exception {
    com.spotify.mobius2.Next<String, String> next =
        com.spotify.mobius2.Next.next("m", effects("f"));

    assertTrue(next.hasModel());
    assertTrue(next.hasEffects());
  }

  @Test
  public void andEffectsFactoriesAreEquivalent() throws Exception {
    com.spotify.mobius2.Next<?, String> a =
        com.spotify.mobius2.Next.next("m", effects("f1", "f2", "f3"));
    com.spotify.mobius2.Next<?, String> b =
        com.spotify.mobius2.Next.next("m", setOf("f1", "f2", "f3"));

    assertEquals(a, b);
  }

  @Test
  public void canMergeInnerEffects() throws Exception {
    com.spotify.mobius2.Next<String, String> outerNext =
        com.spotify.mobius2.Next.next("m", effects("f1", "f2"));
    com.spotify.mobius2.Next<?, String> innerNext = dispatch(effects("f2", "f3"));

    com.spotify.mobius2.Next<String, String> merged =
        com.spotify.mobius2.Next.next(
            outerNext.modelOrElse("fail"), unionSets(innerNext.effects(), outerNext.effects()));

    assertEquals(com.spotify.mobius2.Next.next("m", effects("f1", "f2", "f3")), merged);
  }

  @Test
  public void canMergeInnerEffectsAndModel() throws Exception {
    Set<String> effects = setOf("f1", "f2");
    com.spotify.mobius2.Next<Integer, String> innerNext =
        com.spotify.mobius2.Next.next(1, effects("f2", "f3"));

    com.spotify.mobius2.Next<String, String> merged =
        com.spotify.mobius2.Next.next(
            "m" + innerNext.modelOrElse(0), unionSets(effects, innerNext.effects()));

    assertEquals(com.spotify.mobius2.Next.next("m1", effects("f1", "f2", "f3")), merged);
  }

  @Test
  public void testEquals() throws Exception {
    com.spotify.mobius2.Next<String, String> m1 =
        new AutoValue_Next<>("hi", ImmutableUtil.<String>emptySet());
    com.spotify.mobius2.Next<String, String> m2 = com.spotify.mobius2.Next.next("hi");
    com.spotify.mobius2.Next<String, String> m3 =
        com.spotify.mobius2.Next.next("hi", ImmutableUtil.<String>emptySet());

    com.spotify.mobius2.Next<String, String> n1 =
        new AutoValue_Next<>("hi", ImmutableUtil.setOf("a", "b"));
    com.spotify.mobius2.Next<String, String> n2 =
        com.spotify.mobius2.Next.next("hi", effects("a", "b"));
    com.spotify.mobius2.Next<String, String> n3 =
        com.spotify.mobius2.Next.next("hi", effects("b", "a"));
    com.spotify.mobius2.Next<String, String> n4 =
        com.spotify.mobius2.Next.next("hi", ImmutableUtil.setOf("b", "a"));

    com.spotify.mobius2.Next<String, String> o1 =
        new AutoValue_Next<>("hi", ImmutableUtil.setOf("a", "b", "c"));
    com.spotify.mobius2.Next<String, String> o2 =
        com.spotify.mobius2.Next.next("hi", effects("a", "c", "b"));
    com.spotify.mobius2.Next<String, String> o3 =
        com.spotify.mobius2.Next.next("hi", effects("b", "a", "c"));
    com.spotify.mobius2.Next<String, String> o4 =
        com.spotify.mobius2.Next.next("hi", ImmutableUtil.setOf("c", "b", "a"));

    com.spotify.mobius2.Next<String, String> p1 =
        new AutoValue_Next<>(null, ImmutableUtil.setOf("a", "b", "c"));
    com.spotify.mobius2.Next<String, String> p2 =
        com.spotify.mobius2.Next.dispatch(effects("a", "c", "b"));
    com.spotify.mobius2.Next<String, String> p3 =
        com.spotify.mobius2.Next.dispatch(effects("b", "a", "c"));
    com.spotify.mobius2.Next<String, String> p4 =
        com.spotify.mobius2.Next.dispatch(ImmutableUtil.setOf("c", "b", "a"));

    com.spotify.mobius2.Next<String, String> q1 =
        new AutoValue_Next<>("hey", ImmutableUtil.<String>setOf());
    com.spotify.mobius2.Next<String, String> q2 = com.spotify.mobius2.Next.next("hey");
    com.spotify.mobius2.Next<String, String> q3 =
        com.spotify.mobius2.Next.next("hey", Collections.<String>emptySet());

    com.spotify.mobius2.Next<String, String> r1 =
        new AutoValue_Next<>("hey", ImmutableUtil.setOf("a", "b"));
    com.spotify.mobius2.Next<String, String> r2 =
        com.spotify.mobius2.Next.next("hey", effects("a", "b"));

    com.spotify.mobius2.Next<String, String> s1 =
        new AutoValue_Next<>("hey", ImmutableUtil.setOf("a", "b", "c"));
    com.spotify.mobius2.Next<String, String> s2 =
        com.spotify.mobius2.Next.next("hey", effects("a", "b", "c"));

    new EqualsTester()
        .addEqualityGroup(m1, m2, m3)
        .addEqualityGroup(n1, n2, n3, n4)
        .addEqualityGroup(o1, o2, o3, o4)
        .addEqualityGroup(p1, p2, p3, p4)
        .addEqualityGroup(q1, q2, q3)
        .addEqualityGroup(r1, r2)
        .addEqualityGroup(s1, s2)
        .testEquals();
  }

  // NOTE: the below code doesn't compile with Java 7, but works with Java 8+. This means Java 7
  // users (who don't use DataEnum) will need to provide explicit type parameters, or use
  // intermediate variables for their effect sets.

  // Should compile
  @SuppressWarnings("unused")
  private com.spotify.mobius2.Next<?, Number> canInferFromVarargOnlyEffects() {
    return dispatch(effects((short) 1, 2, (long) 3));
  }

  // Should compile
  @SuppressWarnings("unused")
  private com.spotify.mobius2.Next<?, Number> canInferFromVarargOnlyEffectsSingle() {
    return dispatch(effects((short) 1));
  }

  // Should compile
  @SuppressWarnings("unused")
  private com.spotify.mobius2.Next<?, Number> canInferFromVarargAndEffects() {
    return com.spotify.mobius2.Next.next("m", effects((short) 1, 2, (long) 3));
  }

  // Should compile
  @SuppressWarnings("unused")
  private com.spotify.mobius2.Next<?, Number> canInferFromVarargAndEffectsSingle() {
    return Next.next("m", effects((short) 1));
  }
}
