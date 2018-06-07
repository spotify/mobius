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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

import com.spotify.mobius.Next;
import com.spotify.mobius.Update;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.Matcher;

/**
 * A class to help with Behavior Driven Testing of the {@link Update} function of a Mobius program.
 *
 * @param <M> model type
 * @param <E> events type
 * @param <F> effects type
 */
public class UpdateSpec<M, E, F> {

  private final Update<M, E, F> update;

  public UpdateSpec(Update<M, E, F> update) {
    this.update = checkNotNull(update);
  }

  public When given(M model) {
    return new When(model);
  }

  public final class When {

    private final M model;

    private When(M model) {
      this.model = checkNotNull(model);
    }

    /**
     * Defines the event(s) that should be executed when the test is run. Events are executed in the
     * order supplied.
     *
     * @param event the first events
     * @param events the following events, possibly none
     * @return a {@link Then} instance for the remainder of the spec
     */
    @SafeVarargs
    public final Then<M, F> when(E event, E... events) {
      return new ThenImpl(model, event, events);
    }

    /**
     * Defines the event that should be executed when the test is run. Events are executed in the
     * order supplied. This method is just an alias to {@link #when(E, E...)} for use with Kotlin
     *
     * @param event the first events
     * @return a {@link Then} instance for the remainder of the spec
     */
    public final Then<M, F> whenEvent(E event) {
      return when(event);
    }

    /**
     * Defines the event(s) that should be executed when the test is run. Events are executed in the
     * order supplied. This method is just an alias to {@link #when(E, E...)} for use with Kotlin
     *
     * @param event the first events
     * @param events the following events, possibly none
     * @return a {@link Then} instance for the remainder of the spec
     */
    @SafeVarargs
    public final Then<M, F> whenEvents(E event, E... events) {
      return when(event, events);
    }
  }

  /**
   * The final step in a behavior test. Instances of this class will call your function under test
   * with the previously provided values (i.e. given and when) and will pass the result of the
   * function over to your {@link Assert} implementation. If you choose to call {@code thenError},
   * your function under test will be invoked and any exceptions thrown will be caught and passed on
   * to your {@link AssertionError} implementation. If no exceptions are thrown by the function
   * under test, then an {@link AssertionError} will be thrown to fail the test.
   */
  public interface Then<M, F> {

    /**
     * Runs the specified test and then invokes the {@link Assert} on the {@link Result}.
     *
     * @param assertion to compare the result with
     */
    void then(Assert<M, F> assertion);

    /**
     * Runs the specified test and validates that the last step throws the exception expected by the
     * supplied {@link AssertError}. Note that if the test specification has multiple events, it
     * will fail if the exception is thrown before the execution of the last event.
     *
     * @param assertion an expectation on the exception
     */
    void thenError(AssertError assertion);
  }

  /** Interface for defining your error assertions. */
  public interface AssertError {

    void assertError(Exception e);
  }

  /** Interface for defining your assertions over {@link Next} instances. */
  public interface Assert<M, F> {

    void apply(Result<M, F> result);
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
  public static <M, F> UpdateSpec.Assert<M, F> assertThatNext(Matcher<Next<M, F>>... matchers) {
    return result -> {
      for (Matcher<Next<M, F>> matcher : matchers) {
        assertThat(result.lastNext(), matcher);
      }
    };
  }

  private class ThenImpl implements Then<M, F> {

    private final M model;
    private final List<E> events;

    @SafeVarargs
    private ThenImpl(M model, E event, E... events) {
      this.model = checkNotNull(model);
      this.events = new ArrayList<>(events.length + 1);
      this.events.add(event);
      this.events.addAll(Arrays.asList(events));
    }

    @Override
    public void then(Assert<M, F> assertion) {
      Next<M, F> last = null;
      M lastModel = model;

      for (E event : events) {
        last = update.update(lastModel, event);
        lastModel = last.modelOrElse(lastModel);
      }

      assertion.apply(Result.of(lastModel, checkNotNull(last)));
    }

    @Override
    public void thenError(AssertError assertion) {
      Exception error = null;

      M lastModel = model;

      // play all events up to the last one
      for (int i = 0; i < events.size() - 1; i++) {
        lastModel = update.update(lastModel, events.get(i)).modelOrElse(lastModel);
      }

      // then, do the assertion on the final event
      try {
        update.update(model, events.get(events.size() - 1));
      } catch (Exception e) {
        error = e;
      }

      if (error == null) {
        throw new AssertionError("An exception was expected but was not thrown");
      }
      assertion.assertError(error);
    }
  }
}
