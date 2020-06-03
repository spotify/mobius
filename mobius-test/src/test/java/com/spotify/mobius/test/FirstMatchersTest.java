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
package com.spotify.mobius.test;

import static com.spotify.mobius.Effects.effects;
import static com.spotify.mobius.First.first;
import static com.spotify.mobius.test.FirstMatchers.hasEffects;
import static com.spotify.mobius.test.FirstMatchers.hasModel;
import static com.spotify.mobius.test.FirstMatchers.hasNoEffects;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.spotify.mobius.First;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.Test;

public class FirstMatchersTest {

  private First<String, Integer> first;
  private Matcher<First<String, Integer>> matcher;
  private Description desc;

  @Before
  public void setUp() throws Exception {
    desc = new StringDescription();
  }

  @Test
  public void testHasModelSpecific() throws Exception {
    first = first("a");
    matcher = hasModel(equalTo("a"));

    assertTrue(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("has model: was \"a\"", desc.toString());
  }

  @Test
  public void testHasModelWithValue() throws Exception {
    first = first("a");
    matcher = hasModel("a");

    assertTrue(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("has model: was \"a\"", desc.toString());
  }

  @Test
  public void testHasModelSpecificButWrong() throws Exception {
    first = first("b");
    matcher = hasModel(equalTo("a"));

    assertFalse(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("bad model: was \"b\"", desc.toString());
  }

  @Test
  public void testHasNoEffectsMatch() throws Exception {
    first = first("a");
    matcher = hasNoEffects();

    assertTrue(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("no effects", desc.toString());
  }

  @Test
  public void testHasNoEffectsMismatch() throws Exception {
    first = first("a", effects(1, 2, 3));
    matcher = hasNoEffects();

    assertFalse(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("has effects", desc.toString());
  }

  @Test
  public void testHasEffectsSpecific() throws Exception {
    first = first("a", effects(1, 3, 2));
    matcher = hasEffects(hasItems(1, 2, 3));

    assertTrue(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("has effects: ", desc.toString());
  }

  @Test
  public void testHasEffectsSpecificButWrong() throws Exception {
    first = first("a", effects(1));
    matcher = hasEffects(hasItems(2));

    assertFalse(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("bad effects: a collection containing <2> was <1>", desc.toString());
  }

  @Test
  public void testHasEffectsSpecificButMissing() throws Exception {
    first = first("a");
    matcher = hasEffects(hasItems(1, 2, 3));

    assertFalse(matcher.matches(first));

    matcher.describeMismatch(first, desc);

    assertEquals("no effects", desc.toString());
  }

  @Test
  public void testHasEffectsWithConcreteInstancesMatch() throws Exception {
    first = first("a", effects(94));

    matcher = hasEffects(94);

    assertTrue(matcher.matches(first));
  }

  @Test
  public void testHasEffectsWithConcreteInstancesMismatch() throws Exception {
    first = first("a", effects(94));

    matcher = hasEffects(74);

    assertFalse(matcher.matches(first));
  }

  @Test
  public void testHasEffectsWithConcreteInstancesPartialMatch() throws Exception {
    first = first("a", effects(94, 74));

    matcher = hasEffects(94);

    assertTrue(matcher.matches(first));
  }

  @Test
  public void testHasEffectsWithConcreteInstancesOutOfOrder() throws Exception {
    first = first("a", effects(94, 74, 65));

    matcher = hasEffects(65, 94, 74);

    assertTrue(matcher.matches(first));
  }
}
