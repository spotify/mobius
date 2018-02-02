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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.spotify.mobius.First;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/** Provides utility functions for matching against {@link First} instances. */
public final class FirstMatchers {
  private FirstMatchers() {
    // prevent instantiation
  }

  /**
   * Returns a matcher that matches {@link First} instances with a model that is equal to the
   * supplied one.
   *
   * @param expected the expected model
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<First<M, F>> hasModel(M expected) {
    return hasModel(equalTo(expected));
  }

  /**
   * Returns a matcher that matches {@link First} instances with a model that matches the supplied
   * model matcher.
   *
   * @param matcher the matcher to apply to the model
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<First<M, F>> hasModel(Matcher<M> matcher) {
    return new TypeSafeDiagnosingMatcher<First<M, F>>() {
      @Override
      protected boolean matchesSafely(First<M, F> item, Description mismatchDescription) {
        if (!matcher.matches(item.model())) {
          mismatchDescription.appendText("bad model: ");
          matcher.describeMismatch(item.model(), mismatchDescription);
          return false;

        } else {
          mismatchDescription.appendText("has model: ");
          matcher.describeMismatch(item.model(), mismatchDescription);
          return true;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has a model: ").appendDescriptionOf(matcher);
      }
    };
  }

  /**
   * Returns a matcher that matches {@link First} instances with no effects.
   *
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<First<M, F>> hasNoEffects() {
    return new TypeSafeDiagnosingMatcher<First<M, F>>() {
      @Override
      protected boolean matchesSafely(First<M, F> item, Description mismatchDescription) {
        if (item.hasEffects()) {
          mismatchDescription.appendText("has effects");
          return false;

        } else {
          mismatchDescription.appendText("no effects");
          return true;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("should have no effects");
      }
    };
  }

  /**
   * Returns a matcher that matches {@link First} instances whose effects match the supplied effect
   * matcher.
   *
   * @param matcher the matcher to apply to the effects
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<First<M, F>> hasEffects(Matcher<Iterable<F>> matcher) {
    return new TypeSafeDiagnosingMatcher<First<M, F>>() {
      @Override
      protected boolean matchesSafely(First<M, F> item, Description mismatchDescription) {
        if (!item.hasEffects()) {
          mismatchDescription.appendText("no effects");
          return false;

        } else if (!matcher.matches(item.effects())) {
          mismatchDescription.appendText("bad effects: ");
          matcher.describeMismatch(item.effects(), mismatchDescription);
          return false;

        } else {
          mismatchDescription.appendText("has effects: ");
          matcher.describeMismatch(item.effects(), mismatchDescription);
          return true;
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("has effects: ").appendDescriptionOf(matcher);
      }
    };
  }

  /**
   * Returns a matcher that matches if all the supplied effects are present in the supplied {@link
   * First}, in any order. The {@link First} may have more effects than the ones included.
   *
   * @param effects the effects to match (possibly empty)
   * @param <M> the model type
   * @param <F> the effect type
   * @return a matcher that matches {@link First} instances that include all the supplied effects
   */
  @SafeVarargs
  public static <M, F> Matcher<First<M, F>> hasEffects(F... effects) {
    return hasEffects(hasItems(effects));
  }
}
