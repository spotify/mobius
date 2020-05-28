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
package com.spotify.mobius;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.spotify.mobius.MobiusHooks.ErrorHandler;
import java.util.LinkedList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class MobiusHooksTest {

  private ListAppender<ILoggingEvent> appender;
  private Logger logbackLogger;

  @Before
  public void setUp() throws Exception {
    appender = new ListAppender<>();
    appender.start();

    logbackLogger = (Logger) LoggerFactory.getLogger(MobiusHooks.class);

    logbackLogger.addAppender(appender);
    MobiusHooks.setDefaultErrorHandler();
  }

  @After
  public void tearDown() throws Exception {
    logbackLogger.detachAppender(appender);
  }

  @Test
  public void shouldHaveADefaultHandlerThatLogs() throws Exception {
    Exception expected = new RuntimeException("I'm expected");

    MobiusHooks.handleError(expected);

    assertThat(appender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .containsExactly("Uncaught error");

    assertThat(appender.list)
        .extracting((ILoggingEvent event) -> event.getThrowableProxy().getMessage())
        .containsExactly("I'm expected");
  }

  @Test
  public void shouldAllowChangingTheHandler() throws Exception {
    MyErrorHandler myErrorHandler = new MyErrorHandler();
    MobiusHooks.setErrorHandler(myErrorHandler);

    final RuntimeException theError = new RuntimeException("hey there");

    MobiusHooks.handleError(theError);

    assertThat(myErrorHandler.handledErrors).containsExactly(theError);
    assertThat(appender.list).isEmpty();
  }

  private static class MyErrorHandler implements ErrorHandler {

    private List<Throwable> handledErrors = new LinkedList<>();

    @Override
    public void handleError(Throwable error) {
      handledErrors.add(error);
    }
  }
}
