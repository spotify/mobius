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
import static org.assertj.core.api.Assertions.assertThat;

import com.google.auto.value.AutoValue;
import com.spotify.mobius.First;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.MobiusLoop.Logger;
import com.spotify.mobius.Next;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CompositeLoggerTest {

  private RecordingLogger<String, Integer, String> logger1;
  private RecordingLogger<String, Integer, String> logger2;
  private RecordingLogger<String, Integer, String> logger3;
  private MobiusLoop.Logger<String, Integer, String> underTest;
  private List<String> logEntries;
  private TaggingLogger<String, Integer, String> taggingLogger1;
  private TaggingLogger<String, Integer, String> taggingLogger2;

  @Before
  public void setUp() {
    logger1 = new RecordingLogger<>();
    logger2 = new RecordingLogger<>();
    logger3 = new RecordingLogger<>();
    underTest = CompositeLogger.from(logger1, logger2, logger3);
    logEntries = Collections.synchronizedList(new LinkedList<>());
    taggingLogger1 = new TaggingLogger<>("1", logEntries);
    taggingLogger2 = new TaggingLogger<>("2", logEntries);
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

  @Test
  public void callsLoggersInFifoOrderForBeforeInit() throws Exception {
    underTest = CompositeLogger.from(taggingLogger1, taggingLogger2);

    underTest.beforeInit("moo");

    assertThat(logEntries).containsExactly("1: beforeInit", "2: beforeInit");
  }

  @Test
  public void callsLoggersInLifoOrderForAfterInit() throws Exception {
    underTest = CompositeLogger.from(taggingLogger1, taggingLogger2);

    underTest.afterInit("moo", First.first("!!"));

    assertThat(logEntries).containsExactly("2: afterInit", "1: afterInit");
  }

  @Test
  public void callsLoggersInLifoOrderForExceptionDuringInit() throws Exception {
    underTest = CompositeLogger.from(taggingLogger1, taggingLogger2);

    underTest.exceptionDuringInit("moo", new RuntimeException("bark"));

    assertThat(logEntries).containsExactly("2: exceptionDuringInit", "1: exceptionDuringInit");
  }

  @Test
  public void callsLoggersInFifoOrderForBeforeUpdate() throws Exception {
    underTest = CompositeLogger.from(taggingLogger1, taggingLogger2);

    underTest.beforeUpdate("moo", 1);

    assertThat(logEntries).containsExactly("1: beforeUpdate", "2: beforeUpdate");
  }

  @Test
  public void callsLoggersInLifoOrderForAfterUpdate() throws Exception {
    underTest = CompositeLogger.from(taggingLogger1, taggingLogger2);

    underTest.afterUpdate("moo", 1, Next.next("!!"));

    assertThat(logEntries).containsExactly("2: afterUpdate", "1: afterUpdate");
  }

  @Test
  public void callsLoggersInLifoOrderForExceptionDuringUpdate() throws Exception {
    underTest = CompositeLogger.from(taggingLogger1, taggingLogger2);

    underTest.exceptionDuringUpdate("moo", 1, new RuntimeException("bark"));

    assertThat(logEntries).containsExactly("2: exceptionDuringUpdate", "1: exceptionDuringUpdate");
  }

  private void assertTestCaseLogged(LogEvent testCase) {
    logger1.assertLogEvents(testCase);
    logger2.assertLogEvents(testCase);
    logger3.assertLogEvents(testCase);
  }

  private static class RecordingLogger<M, E, F> implements MobiusLoop.Logger<M, E, F> {

    private final List<LogEvent> events = new ArrayList<>();

    void assertLogEvents(LogEvent... events) {
      assertThat(this.events).containsExactly(events);
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

  private static class TaggingLogger<M, E, F> implements MobiusLoop.Logger<M, E, F> {
    private final String tag;
    private final List<String> logEntries;

    private TaggingLogger(String tag, List<String> logEntries) {
      this.tag = tag;
      this.logEntries = logEntries;
    }

    @Override
    public void beforeInit(M model) {
      logEntries.add(String.format("%s: beforeInit", tag));
    }

    @Override
    public void afterInit(M model, First<M, F> result) {
      logEntries.add(String.format("%s: afterInit", tag));
    }

    @Override
    public void exceptionDuringInit(M model, Throwable exception) {
      logEntries.add(String.format("%s: exceptionDuringInit", tag));
    }

    @Override
    public void beforeUpdate(M model, E event) {
      logEntries.add(String.format("%s: beforeUpdate", tag));
    }

    @Override
    public void afterUpdate(M model, E event, Next<M, F> result) {
      logEntries.add(String.format("%s: afterUpdate", tag));
    }

    @Override
    public void exceptionDuringUpdate(M model, E event, Throwable exception) {
      logEntries.add(String.format("%s: exceptionDuringUpdate", tag));
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
