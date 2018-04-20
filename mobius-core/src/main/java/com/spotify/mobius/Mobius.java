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

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.EffectRouter.Builder;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Producer;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.runners.WorkRunners;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;

public final class Mobius {
  private Mobius() {
    // prevent instantiation
  }

  private static final Init<?, ?> NOOP_INIT =
      new Init<Object, Object>() {
        @Nonnull
        @Override
        public First<Object, Object> init(Object model) {
          return First.first(model);
        }
      };

  private static final EventSource<?> NOOP_EVENT_SOURCE =
      new EventSource<Object>() {
        @Nonnull
        @Override
        public Disposable subscribe(Consumer<Object> eventConsumer) {
          return new Disposable() {
            @Override
            public void dispose() {}
          };
        }
      };

  private static final MobiusLoop.Logger<?, ?, ?> NOOP_LOGGER =
      new MobiusLoop.Logger<Object, Object, Object>() {
        @Override
        public void beforeInit(Object model) {
          /* noop */
        }

        @Override
        public void afterInit(Object model, First<Object, Object> result) {
          /* noop */
        }

        @Override
        public void exceptionDuringInit(Object model, Throwable exception) {
          System.err.println("error initialising from model: '" + model + "' - " + exception);
          exception.printStackTrace(System.err);
        }

        @Override
        public void beforeUpdate(Object model, Object event) {
          /* noop */
        }

        @Override
        public void afterUpdate(Object model, Object event, Next<Object, Object> result) {
          /* noop */
        }

        @Override
        public void exceptionDuringUpdate(Object model, Object event, Throwable exception) {
          System.err.println(
              "error updating model: '" + model + "' with event: '" + event + "' - " + exception);
          exception.printStackTrace(System.err);
        }
      };

  /**
   * Create a {@link MobiusLoop.Builder} to help you configure a MobiusLoop before starting it.
   *
   * <p>Once done configuring the loop you can start the loop using {@link
   * MobiusLoop.Factory#startFrom(Object)}.
   *
   * @param update the {@link Update} function of the loop
   * @param effectHandler the {@link Connectable} effect handler of the loop
   * @return a {@link MobiusLoop.Builder} instance that you can further configure before starting
   *     the loop
   */
  public static <M, E, F> MobiusLoop.Builder<M, E, F> loop(
      Update<M, E, F> update, Connectable<F, E> effectHandler) {

    //noinspection unchecked
    return new Builder<>(
        update,
        effectHandler,
        (Init<M, F>) NOOP_INIT,
        (EventSource<E>) NOOP_EVENT_SOURCE,
        (MobiusLoop.Logger<M, E, F>) NOOP_LOGGER,
        new Producer<WorkRunner>() {
          @Nonnull
          @Override
          public WorkRunner get() {
            return WorkRunners.from(Executors.newSingleThreadExecutor(Builder.THREAD_FACTORY));
          }
        },
        new Producer<WorkRunner>() {
          @Nonnull
          @Override
          public WorkRunner get() {
            return WorkRunners.from(Executors.newCachedThreadPool(Builder.THREAD_FACTORY));
          }
        });
  }

  /**
   * Create a {@link MobiusLoop.Controller} that allows you to start, stop, and restart MobiusLoops.
   *
   * @param loopFactory a factory for creating loops
   * @param defaultModel the model the controller should start from
   * @return a new controller
   */
  public static <M, E, F> MobiusLoop.Controller<M, E> controller(
      MobiusLoop.Factory<M, E, F> loopFactory, M defaultModel) {
    return new MobiusLoopController<>(loopFactory, defaultModel, WorkRunners.immediate());
  }

  /**
   * Create a {@link MobiusLoop.Controller} that allows you to start, stop, and restart MobiusLoops.
   *
   * @param loopFactory a factory for creating loops
   * @param defaultModel the model the controller should start from
   * @param modelRunner the WorkRunner to use when observing model changes
   * @return a new controller
   */
  public static <M, E, F> MobiusLoop.Controller<M, E> controller(
      MobiusLoop.Factory<M, E, F> loopFactory, M defaultModel, WorkRunner modelRunner) {
    return new MobiusLoopController<>(loopFactory, defaultModel, modelRunner);
  }

  @Nonnull
  public static <F, E> EffectRouterBuilder<F, E> subtypeEffectHandler() {
    return new EffectRouter.Builder<>();
  }

  private static final class Builder<M, E, F> implements MobiusLoop.Builder<M, E, F> {

    private static final MyThreadFactory THREAD_FACTORY = new MyThreadFactory();

    private final Update<M, E, F> update;
    private final Connectable<F, E> effectHandler;
    private final Init<M, F> init;
    private final EventSource<E> eventSource;
    private final Producer<WorkRunner> eventRunner;
    private final Producer<WorkRunner> effectRunner;
    private final MobiusLoop.Logger<M, E, F> logger;

    private Builder(
        Update<M, E, F> update,
        Connectable<F, E> effectHandler,
        Init<M, F> init,
        EventSource<E> eventSource,
        MobiusLoop.Logger<M, E, F> logger,
        Producer<WorkRunner> eventRunner,
        Producer<WorkRunner> effectRunner) {
      this.update = checkNotNull(update);
      this.effectHandler = checkNotNull(effectHandler);
      this.init = checkNotNull(init);
      this.eventSource = checkNotNull(eventSource);
      this.eventRunner = checkNotNull(eventRunner);
      this.effectRunner = checkNotNull(effectRunner);
      this.logger = checkNotNull(logger);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> init(Init<M, F> init) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> eventSource(EventSource<E> eventSource) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> logger(MobiusLoop.Logger<M, E, F> logger) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> eventRunner(Producer<WorkRunner> eventRunner) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop.Builder<M, E, F> effectRunner(Producer<WorkRunner> effectRunner) {
      return new Builder<>(
          update, effectHandler, init, eventSource, logger, eventRunner, effectRunner);
    }

    @Override
    @Nonnull
    public MobiusLoop<M, E, F> startFrom(M startModel) {
      LoggingInit<M, F> loggingInit = new LoggingInit<>(init, logger);
      LoggingUpdate<M, E, F> loggingUpdate = new LoggingUpdate<>(update, logger);

      return MobiusLoop.create(
          MobiusStore.create(loggingInit, loggingUpdate, checkNotNull(startModel)),
          effectHandler,
          eventSource,
          checkNotNull(eventRunner.get()),
          checkNotNull(effectRunner.get()));
    }

    private static class MyThreadFactory implements ThreadFactory {

      private static final AtomicLong threadCount = new AtomicLong(0);

      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread = Executors.defaultThreadFactory().newThread(checkNotNull(runnable));

        thread.setName(
            String.format(Locale.ENGLISH, "mobius-thread-%d", threadCount.incrementAndGet()));

        return thread;
      }
    }
  }
}
