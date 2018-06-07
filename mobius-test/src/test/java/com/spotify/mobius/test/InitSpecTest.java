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

import static com.spotify.mobius.Effects.effects;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.spotify.mobius.First;
import com.spotify.mobius.Init;
import org.junit.Before;
import org.junit.Test;

public class InitSpecTest {
  private static final Init<String, Integer> INIT =
      model -> {
        if ("bad model".equals(model)) {
          throw new IllegalStateException("Bad Bad Model!");
        }
        return First.first(model.concat(model), effects(1, 2, 3));
      };

  private InitSpec<String, Integer> initSpec;

  @Before
  public void setUp() throws Exception {
    initSpec = new InitSpec<>(INIT);
  }

  @Test
  public void initTests() throws Exception {
    initSpec
        .when("Hello World")
        .then(
            first -> {
              assertThat(first.model(), is("Hello WorldHello World"));
              assertThat(first.effects(), contains(1, 2, 3));
            });

    initSpec
        .when("bad model")
        .thenError(error -> assertThat(error, instanceOf(IllegalStateException.class)));
  }

  @Test
  public void whenAndWhenModelYieldsSameResults() throws Exception {
    InitSpec.Assert<String, Integer> successAssertion =
        first -> {
          assertThat(first.model(), is("Hello WorldHello World"));
          assertThat(first.effects(), contains(1, 2, 3));
        };

    initSpec.when("Hello World").then(successAssertion);

    initSpec.whenInit("Hello World").then(successAssertion);

    InitSpec.AssertError failureAssertion =
        error -> assertThat(error, instanceOf(IllegalStateException.class));
    initSpec.when("bad model").thenError(failureAssertion);

    initSpec.whenInit("bad model").thenError(failureAssertion);
  }

  @Test
  public void shouldFailIfExpectedErrorDoesntHappen() throws Exception {
    assertThatThrownBy(
            () ->
                initSpec
                    .when("no crash here")
                    .thenError(error -> assertThat(error, instanceOf(IllegalStateException.class))))
        .isInstanceOf(AssertionError.class)
        .hasMessage("An exception was expected but was not thrown");
  }
}
