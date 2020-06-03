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
import static com.spotify.mobius.test.NextMatchers.hasEffects;
import static com.spotify.mobius.test.NextMatchers.hasModel;
import static com.spotify.mobius.test.NextMatchers.hasNothing;
import static com.spotify.mobius.test.UpdateSpec.assertThatNext;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.spotify.mobius.Next;
import com.spotify.mobius.Update;
import org.junit.Before;
import org.junit.Test;

public class UpdateSpecTest {

  private static final Update<String, String, Integer> UPDATE =
      (model, event) -> {
        if (!"HELLO".equals(model)) {
          return Next.noChange();
        }
        return Next.next(model.toUpperCase(), effects(1, 2));
      };
  private static final UpdateSpec<String, String, Integer> CRASH_SPEC =
      new UpdateSpec<>(
          (model, event) -> {
            if (event.equals("crash")) {
              throw new RuntimeException("expected");
            }

            return Next.next(model + "-" + event);
          });

  private UpdateSpec<String, String, Integer> updateSpec;

  @Before
  public void setUp() throws Exception {
    updateSpec = new UpdateSpec<>(UPDATE);
  }

  @Test
  public void updateTests() throws Exception {
    updateSpec
        .given("HELLO")
        .when("anything")
        .then(assertThatNext(hasModel("HELLO"), hasEffects(1, 2)));

    updateSpec.given("anything but hello").when("any event").then(assertThatNext(hasNothing()));
  }

  @Test
  public void whenAndWhenEventYieldSameResult() {
    UpdateSpec.Assert<String, Integer> assertion =
        assertThatNext(hasModel("HELLO"), hasEffects(1, 2));

    updateSpec.given("HELLO").when("anything").then(assertion);

    updateSpec.given("HELLO").whenEvent("anything").then(assertion);
  }

  @Test
  public void shouldSupportWhenMultipleEvents() throws Exception {
    updateSpec =
        new UpdateSpec<>(
            (model, event) ->
                Next.next(
                    model + " - " + event, event.equals("three") ? effects(8, 7, 4) : effects()));

    updateSpec
        .given("init")
        .when("one", "two", "three")
        .then(
            result -> {
              assertThat(result.model(), is("init - one - two - three"));
              assertThat(result.lastNext(), hasEffects(7, 8, 4));
            });

    updateSpec
        .given("init")
        .whenEvents("one", "two", "three")
        .then(
            result -> {
              assertThat(result.model(), is("init - one - two - three"));
              assertThat(result.lastNext(), hasEffects(7, 8, 4));
            });
  }

  @Test
  public void shouldFailAsExpectedForMultipleEventsLast() throws Exception {
    updateSpec = new UpdateSpec<>((model, event) -> Next.next(model + " - " + event));

    assertThatThrownBy(
            () ->
                updateSpec
                    .given("init")
                    .when("one", "two", "three")
                    .then(last -> assertThat(last.model(), is("wrong"))))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "Expected: is \"wrong\"\n" + "     but: was \"init - one - two - three\"");
  }

  @Test
  public void shouldSupportErrorValidation() throws Exception {
    updateSpec = CRASH_SPEC;

    updateSpec
        .given("hi")
        .when("ok", "crash")
        .thenError(e -> assertThat(e.getMessage(), is("expected")));
  }

  @Test
  public void shouldOnlyAcceptErrorsInLastStep() throws Exception {
    updateSpec = CRASH_SPEC;

    // should throw the crash exception from the first event and never get to the 'thenError' clause
    assertThatThrownBy(
            () ->
                updateSpec
                    .given("hi")
                    .when("crash", "crash")
                    .thenError(e -> assertThat(e.getMessage(), is("expected"))))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("expected");
  }

  @Test
  public void shouldFailIfExpectedErrorDoesntHappen() throws Exception {
    assertThatThrownBy(
            () ->
                updateSpec
                    .given("hi")
                    .when("no crash here")
                    .thenError(error -> assertThat(error, instanceOf(IllegalStateException.class))))
        .isInstanceOf(AssertionError.class)
        .hasMessage("An exception was expected but was not thrown");
  }
}
