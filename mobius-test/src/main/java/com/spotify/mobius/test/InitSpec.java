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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

import com.spotify.mobius.First;
import com.spotify.mobius.Init;
import org.hamcrest.Matcher;

/**
 * A class to help with Behavior Driven Testing of the {@link Init} function of a Mobius program.
 *
 * @param <M> model type
 * @param <F> effects type
 */
public class InitSpec<M, F> {

  private final Init<M, F> init;

  public InitSpec(Init<M, F> init) {
    this.init = checkNotNull(init);
  }

  public Then<M, F> when(M model) {
    checkNotNull(model);
    return new Then<M, F>() {
      @Override
      public void then(Assert<M, F> assertion) {
        assertion.assertFirst(init.init(model));
      }

      @Override
      public void thenError(AssertError assertion) {
        Exception error = null;
        try {
          init.init(model);
        } catch (Exception e) {
          error = e;
        }

        if (error == null) {
          throw new AssertionError("An exception was expected but was not thrown");
        }
        assertion.assertError(error);
      }
    };
  }

  /** An alias for {@link #when(Object)} to be used with Kotlin */
  public Then<M, F> whenInit(M model) {
    return when(model);
  }

  /**
   * The final step in a behavior test. Instances of this class will call your function under test
   * with the previously provided values (i.e. given and when) and will pass the result of the
   * function over to your {@link Assert} implementation. If you choose to call {@code thenError},
   * your function under test will be invoked and any exceptions thrown will be caught and passed on
   * to your {@link AssertError} implementation. If no exceptions are thrown by the function under
   * test, then an {@link AssertError} will be thrown to fail the test.
   */
  public interface Then<M, F> {

    /**
     * Runs the specified test and then runs the {@link Assert} on the resulting {@link First}.
     *
     * @param assertion to compare the result with
     */
    void then(Assert<M, F> assertion);

    /**
     * Runs the specified test and validates that it throws the exception expected by the supplied
     * {@link AssertError}.
     *
     * @param assertion an expectation on the exception
     */
    void thenError(AssertError assertion);
  }

  /** Interface for defining your error assertions. */
  public interface AssertError {

    void assertError(Exception e);
  }

  /** Interface for defining your assertions over {@link First} instances. */
  public interface Assert<M, F> {

    void assertFirst(First<M, F> first);
  }

  /**
   * Convenience function for creating assertions.
   *
   * @param matchers an array of matchers, all of which must match
   * @param <M> the model type
   * @param <F> the effect type
   * @return an {@link Assert} that applies all the matchers
   */
  @SafeVarargs
  public static <M, F> InitSpec.Assert<M, F> assertThatFirst(Matcher<First<M, F>>... matchers) {
    return first -> {
      for (Matcher<First<M, F>> matcher : matchers) {
        assertThat(first, matcher);
      }
    };
  }
}
