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
package com.spotify.mobius.extras;

import static com.spotify.mobius.Next.next;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.auto.value.AutoValue;
import com.spotify.mobius.First;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.Next;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CompositeLoggerTest {

  private RecordingLogger<String, Integer, String> logger1;
  private RecordingLogger<String, Integer, String> logger2;
  private RecordingLogger<String, Integer, String> logger3;
  private MobiusLoop.Logger<String, Integer, String> underTest;

  @Before
  public void setUp() {
    logger1 = new RecordingLogger<>();
    logger2 = new RecordingLogger<>();
    logger3 = new RecordingLogger<>();
    underTest = CompositeLogger.from(logger1, logger2, logger3);
  }

  @Test
  public void delegatesBeforeInitToAllLoggers() {
    BeforeInit<String> testCase = BeforeInit.create("Hello");
    underTest.beforeInit(testCase.model());
    assertTestCaseLogged(testCase);
  }

  @Test
  public void delegatesAfterInitToAllLoggers() {
    AfterInit<String, String> testCase =
        AfterInit.create("Hello", First.<String, String>first("World"));
    underTest.afterInit(testCase.model(), testCase.first());
    assertTestCaseLogged(testCase);
  }

  @Test
  public void delegatesExceptionDuringInitToAllLoggers() throws Exception {
    ExceptionDuringInit<String> testCase =
        ExceptionDuringInit.create("I'm broken", Exception.class);
    underTest.exceptionDuringInit(testCase.model(), testCase.createException());
    assertTestCaseLogged(testCase);
  }

  @Test
  public void delegatesBeforeUpdateToAllLoggers() {
    BeforeUpdate<String, Integer> testCase = BeforeUpdate.create("Hello", 5);
    underTest.beforeUpdate(testCase.model(), testCase.event());
    assertTestCaseLogged(testCase);
  }

  @Test
  public void delegatesAfterUpdateToAllLoggers() {
    AfterUpdate<String, Integer, String> testCase =
        AfterUpdate.create("Hello", 5, next("World", singleton("test")));
    underTest.afterUpdate(testCase.model(), testCase.event(), testCase.next());
    assertTestCaseLogged(testCase);
  }

  @Test
  public void delegatesExceptionDuringUpdateToAllLoggers()
      throws InstantiationException, IllegalAccessException {
    ExceptionDuringUpdate<String, Integer> testCase =
        ExceptionDuringUpdate.create("Something bad happened", 6, Exception.class);
    underTest.exceptionDuringUpdate(testCase.model(), testCase.event(), testCase.createException());
    assertTestCaseLogged(testCase);
  }

  private void assertTestCaseLogged(LogEvent testCase) {
    logger1.assertLogEvents(testCase);
    logger2.assertLogEvents(testCase);
    logger3.assertLogEvents(testCase);
  }

  private static class RecordingLogger<M, E, F> implements MobiusLoop.Logger<M, E, F> {

    private final List<LogEvent> events = new ArrayList<>();

    public void assertLogEvents(LogEvent... events) {
      assertThat(this.events, is(equalTo(Arrays.asList(events))));
    }

    @Override
    public void beforeInit(M model) {
      events.add(BeforeInit.create(model));
    }

    @Override
    public void afterInit(M model, First<M, F> result) {
      events.add(AfterInit.create(model, result));
    }

    @Override
    public void exceptionDuringInit(M model, Throwable exception) {
      events.add(ExceptionDuringInit.create(model, exception.getClass()));
    }

    @Override
    public void beforeUpdate(M model, E event) {
      events.add(BeforeUpdate.create(model, event));
    }

    @Override
    public void afterUpdate(M model, E event, Next<M, F> result) {
      events.add(AfterUpdate.create(model, event, result));
    }

    @Override
    public void exceptionDuringUpdate(M model, E event, Throwable exception) {
      events.add(ExceptionDuringUpdate.create(model, event, exception.getClass()));
    }
  }

  private interface LogEvent {}

  @AutoValue
  abstract static class BeforeInit<M> implements LogEvent {
    abstract M model();

    public static <M> BeforeInit<M> create(M model) {
      return new AutoValue_CompositeLoggerTest_BeforeInit<>(model);
    }
  }

  @AutoValue
  abstract static class AfterInit<M, F> implements LogEvent {
    abstract M model();

    abstract First<M, F> first();

    public static <M, F> AfterInit<M, F> create(M model, First<M, F> first) {
      return new AutoValue_CompositeLoggerTest_AfterInit<>(model, first);
    }
  }

  @AutoValue
  abstract static class ExceptionDuringInit<M> implements LogEvent {
    abstract M model();

    abstract Class exceptionClazz();

    public static <M> ExceptionDuringInit<M> create(M model, Class exceptionClazz) {
      return new AutoValue_CompositeLoggerTest_ExceptionDuringInit<>(model, exceptionClazz);
    }

    Throwable createException() throws IllegalAccessException, InstantiationException {
      return (Throwable) exceptionClazz().newInstance();
    }
  }

  @AutoValue
  abstract static class BeforeUpdate<M, E> implements LogEvent {
    abstract M model();

    abstract E event();

    public static <M, E> BeforeUpdate<M, E> create(M model, E event) {
      return new AutoValue_CompositeLoggerTest_BeforeUpdate<>(model, event);
    }
  }

  @AutoValue
  abstract static class AfterUpdate<M, E, F> implements LogEvent {
    abstract M model();

    abstract E event();

    abstract Next<M, F> next();

    public static <M, E, F> AfterUpdate<M, E, F> create(M model, E event, Next<M, F> next) {
      return new AutoValue_CompositeLoggerTest_AfterUpdate<>(model, event, next);
    }
  }

  @AutoValue
  abstract static class ExceptionDuringUpdate<M, E> implements LogEvent {
    abstract M model();

    abstract E event();

    abstract Class exceptionClazz();

    public static <M, E> ExceptionDuringUpdate<M, E> create(
        M model, E event, Class exceptionClazz) {
      return new AutoValue_CompositeLoggerTest_ExceptionDuringUpdate<>(
          model, event, exceptionClazz);
    }

    Throwable createException() throws IllegalAccessException, InstantiationException {
      return (Throwable) exceptionClazz().newInstance();
    }
  }
}
