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
package com.spotify.mobius2.test;

import static com.spotify.mobius2.Effects.effects;
import static com.spotify.mobius2.Next.dispatch;
import static com.spotify.mobius2.Next.next;
import static com.spotify.mobius2.Next.noChange;
import static com.spotify.mobius2.test.NextMatchers.hasEffects;
import static com.spotify.mobius2.test.NextMatchers.hasModel;
import static com.spotify.mobius2.test.NextMatchers.hasNoEffects;
import static com.spotify.mobius2.test.NextMatchers.hasNoModel;
import static com.spotify.mobius2.test.NextMatchers.hasNothing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.spotify.mobius2.Next;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.Test;

public class NextMatchersTest {

  private Next<String, Integer> next;
  private Matcher<Next<String, Integer>> matcher;
  private Description desc;

  @Before
  public void setUp() throws Exception {
    desc = new StringDescription();
  }

  @Test
  public void testHasNothing() throws Exception {
    matcher = hasNothing();

    assertFalse(matcher.matches(null));

    assertFalse(matcher.matches(next("1234")));

    assertFalse(matcher.matches(dispatch(effects("f1"))));

    assertFalse(matcher.matches(next("123", effects("f1"))));

    assertTrue(matcher.matches(noChange()));
  }

  @Test
  public void testHasModel() throws Exception {
    next = next("a");
    matcher = hasModel();

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasModelFail() throws Exception {
    next = noChange();
    matcher = hasModel();

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("it had no model", desc.toString());
  }

  @Test
  public void testHasNoModel() throws Exception {
    next = dispatch(effects(1, 2));
    matcher = hasNoModel();

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasNoModelMismatch() throws Exception {
    next = next("a");
    matcher = hasNoModel();

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("it had a model: a", desc.toString());
  }

  @Test
  public void testHasModelSpecific() throws Exception {
    next = next("a");
    matcher = hasModel(equalTo("a"));

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasModelWithValue() throws Exception {
    next = next("a");
    matcher = hasModel("a");

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasModelSpecificButWrong() throws Exception {
    next = next("b");
    matcher = hasModel(equalTo("a"));

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("the model was \"b\"", desc.toString());
  }

  @Test
  public void testHasModelSpecificButMissing() throws Exception {
    next = noChange();
    matcher = hasModel(equalTo("a"));

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("it had no model", desc.toString());
  }

  @Test
  public void testHasNoEffectsMatch() throws Exception {
    next = noChange();
    matcher = hasNoEffects();

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasNoEffectsMismatch() throws Exception {
    next = dispatch(effects(1, 2, 3));
    matcher = hasNoEffects();

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("it had effects: [1, 2, 3]", desc.toString());
  }

  @Test
  public void testHasEffectsSpecific() throws Exception {
    next = dispatch(effects(1, 3, 2));
    matcher = hasEffects(hasItems(1, 2, 3));

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasEffectsSpecificButWrong() throws Exception {
    next = dispatch(effects(1));
    matcher = hasEffects(hasItems(2));

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("the effects were a collection containing <2> was <1>", desc.toString());
  }

  @Test
  public void testHasEffectsSpecificButMissing() throws Exception {
    next = noChange();
    matcher = hasEffects(hasItems(1, 2, 3));

    assertFalse(matcher.matches(next));

    matcher.describeMismatch(next, desc);

    assertEquals("it had no effects", desc.toString());
  }

  @Test
  public void testHasEffectsWithConcreteInstancesMatch() throws Exception {
    next = dispatch(effects(94));

    matcher = hasEffects(94);

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasEffectsWithConcreteInstancesMismatch() throws Exception {
    next = dispatch(effects(94));

    matcher = hasEffects(74);

    assertFalse(matcher.matches(next));
  }

  @Test
  public void testHasEffectsWithConcreteInstancesPartialMatch() throws Exception {
    next = dispatch(effects(94, 74));

    matcher = hasEffects(94);

    assertTrue(matcher.matches(next));
  }

  @Test
  public void testHasEffectsWithConcreteInstancesOutOfOrder() throws Exception {
    next = dispatch(effects(94, 74, 65));

    matcher = hasEffects(65, 94, 74);

    assertTrue(matcher.matches(next));
  }
}
