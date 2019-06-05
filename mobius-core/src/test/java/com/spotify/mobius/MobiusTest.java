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

import static com.spotify.mobius.Effects.effects;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.ImmediateWorkRunner;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.runners.WorkRunners;
import com.spotify.mobius.test.SimpleConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.awaitility.Duration;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class MobiusTest {

  private static final Update<String, Integer, Boolean> UPDATE =
      new Update<String, Integer, Boolean>() {
        @Nonnull
        @Override
        public Next<String, Boolean> update(String model, Integer event) {
          return Next.next(model + String.valueOf(event), effects((Boolean) (event % 2 == 0)));
        }
      };

  private static final Connectable<Boolean, Integer> HANDLER =
      new Connectable<Boolean, Integer>() {
        @Nonnull
        @Override
        public Connection<Boolean> connect(final Consumer<Integer> output) {
          return new SimpleConnection<Boolean>() {
            @Override
            public void accept(Boolean value) {
              if (value) {
                output.accept(3);
              }
            }
          };
        }
      };
  private static final String MY_MODEL = "start";
  private MobiusLoop<String, Integer, Boolean> loop;

  @Test
  public void shouldInstantiateWithMinimumParams() throws Exception {
    loop = Mobius.loop(UPDATE, HANDLER).startFrom(MY_MODEL);

    loop.dispatchEvent(8);

    await().atMost(Duration.ONE_SECOND).until(() -> loop.getMostRecentModel(), is("start83"));
  }

  @Test
  public void shouldPermitUsingCustomInit() throws Exception {
    Init<String, Boolean> init =
        new Init<String, Boolean>() {
          @Nonnull
          @Override
          public First<String, Boolean> init(String model) {
            return First.first(model, effects(true));
          }
        };

    loop = Mobius.loop(UPDATE, HANDLER).init(init).startFrom(MY_MODEL);

    loop.dispatchEvent(3);

    await().atMost(Duration.ONE_SECOND).until(() -> "start33".equals(loop.getMostRecentModel()));
  }

  @Test
  public void shouldPermitUsingCustomEffectRunner() throws Exception {
    TestableWorkRunner runner = new TestableWorkRunner();
    loop = Mobius.loop(UPDATE, HANDLER).effectRunner(() -> runner).startFrom(MY_MODEL);

    loop.dispatchEvent(3);

    await().atMost(Duration.ONE_SECOND).until(() -> runner.runCounter.get() == 1);
  }

  @Test
  public void shouldPermitUsingCustomEventRunner() throws Exception {
    TestableWorkRunner runner = new TestableWorkRunner();
    loop = Mobius.loop(UPDATE, HANDLER).eventRunner(() -> runner).startFrom(MY_MODEL);

    loop.dispatchEvent(3);

    // 2 because the initial model dispatch is run on the event runner
    await().atMost(Duration.ONE_SECOND).until(() -> runner.runCounter.get() == 2);
  }

  @Test
  public void shouldPermitUsingEventSource() throws Exception {
    TestEventSource eventSource = new TestEventSource();

    loop =
        Mobius.loop(UPDATE, HANDLER)
            .eventRunner(WorkRunners::immediate)
            .eventSource(eventSource)
            .startFrom(MY_MODEL);

    eventSource.consumer.accept(7);

    await().atMost(Duration.ONE_SECOND).until(() -> loop.getMostRecentModel(), is("start7"));
  }

  @Test
  public void shouldPermitUsingConnectablesAsAnEventSource() throws Exception {
    ConnectableTestEventSource eventSource = new ConnectableTestEventSource();

    loop =
        Mobius.loop(UPDATE, HANDLER)
            .eventRunner(WorkRunners::immediate)
            .eventSource(eventSource)
            .startFrom(MY_MODEL);

    eventSource.consumer.accept(7);

    await().atMost(Duration.ONE_SECOND).until(() -> loop.getMostRecentModel(), is("start7"));
  }

  @Test
  public void shouldPermitUsingCustomLogger() throws Exception {
    TestLogger logger = new TestLogger();

    loop =
        Mobius.loop(UPDATE, HANDLER)
            .logger(logger)
            .eventRunner(ImmediateWorkRunner::new)
            .effectRunner(ImmediateWorkRunner::new)
            .startFrom(MY_MODEL);

    loop.dispatchEvent(7);

    assertThat(
        logger.history,
        contains(
            "before init: start",
            "after init: start, First{model=start, effects=[]}",
            "before update: start, 7",
            "after update: start, 7, Next{model=start7, effects=[false]}"));
  }

  @Test
  public void shouldSupportCreatingFactory() throws Exception {
    MobiusLoop.Factory<String, Integer, Boolean> factory = Mobius.loop(UPDATE, HANDLER);

    loop = factory.startFrom("resume");

    loop.dispatchEvent(97);

    await().atMost(Duration.ONE_SECOND).until(() -> loop.getMostRecentModel(), is("resume97"));
  }

  @Test
  public void shouldSupportCreatingMultipleLoops() throws Exception {
    MobiusLoop.Factory<String, Integer, Boolean> factory = Mobius.loop(UPDATE, HANDLER);

    // one
    loop = factory.startFrom("first");
    loop.dispatchEvent(97);
    await().atMost(Duration.ONE_SECOND).until(() -> loop.getMostRecentModel(), is("first97"));
    loop.dispose();

    // then another one
    loop = factory.startFrom("second");
    loop.dispatchEvent(97);
    await().atMost(Duration.ONE_SECOND).until(() -> loop.getMostRecentModel(), is("second97"));
  }

  private static class TestableWorkRunner implements WorkRunner {

    private final AtomicInteger runCounter = new AtomicInteger();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void post(Runnable runnable) {
      try {
        executorService.submit(runnable).get();
        runCounter.incrementAndGet();
        LoggerFactory.getLogger(TestableWorkRunner.class).debug("runcounter: " + runCounter.get());
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void dispose() {}
  }

  private static class ConnectableTestEventSource implements Connectable<String, Integer> {

    Consumer<Integer> consumer;

    @Nonnull
    @Override
    public Connection<String> connect(Consumer<Integer> output)
        throws ConnectionLimitExceededException {
      this.consumer = output;
      return new Connection<String>() {
        @Override
        public void accept(String value) {}

        @Override
        public void dispose() {}
      };
    }
  }

  private static class TestEventSource implements EventSource<Integer> {

    private Consumer<Integer> consumer;

    @Nonnull
    @Override
    public Disposable subscribe(Consumer<Integer> eventConsumer) {
      consumer = eventConsumer;
      return new Disposable() {
        @Override
        public void dispose() {
          // do nothing
        }
      };
    }
  }

  private static class TestLogger implements MobiusLoop.Logger<String, Integer, Boolean> {

    final List<String> history = new ArrayList<>();

    @Override
    public void beforeInit(String model) {
      history.add(String.format("before init: %s", model));
    }

    @Override
    public void afterInit(String model, First<String, Boolean> result) {
      history.add(String.format("after init: %s, %s", model, result));
    }

    @Override
    public void exceptionDuringInit(String model, Throwable exception) {
      history.add(String.format("init error: %s, %s", model, exception));
    }

    @Override
    public void beforeUpdate(String model, Integer event) {
      history.add(String.format("before update: %s, %s", model, event));
    }

    @Override
    public void afterUpdate(String model, Integer event, Next<String, Boolean> result) {
      history.add(String.format("after update: %s, %s, %s", model, event, result));
    }

    @Override
    public void exceptionDuringUpdate(String model, Integer event, Throwable exception) {
      history.add(String.format("update error: %s, %s, %s", model, event, exception));
    }
  }
}
