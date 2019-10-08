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

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Producer;
import com.spotify.mobius.runners.WorkRunner;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is the main loop for Mobius.
 *
 * <p>It hooks up all the different parts of the main Mobius loop, and dispatches messages
 * internally on the appropriate executors.
 */
public class MobiusLoop<M, E, F> implements Disposable {

  @Nonnull private final Connection<E> safeOnEventReceived;
  @Nonnull private final Connection<F> safeOnEffectReceived;

  @Nonnull private final MessageDispatcher<E> eventDispatcher;
  @Nonnull private final MessageDispatcher<F> effectDispatcher;

  @Nonnull private final EventProcessor<M, E, F> eventProcessor;
  @Nonnull private final Connection<F> effectConsumer;
  @Nonnull private final Connection<M> eventSourceModelConsumer;

  @Nonnull private final List<Consumer<M>> modelObservers = new CopyOnWriteArrayList<>();

  @Nullable private volatile M mostRecentModel;

  private volatile boolean disposed;

  static <M, E, F> MobiusLoop<M, E, F> create(
      Update<M, E, F> update,
      M startModel,
      Iterable<F> startEffects,
      Connectable<F, E> effectHandler,
      Connectable<M, E> eventSource,
      WorkRunner eventRunner,
      WorkRunner effectRunner) {

    return new MobiusLoop<>(
        new EventProcessor.Factory<>(
            MobiusStore.create(checkNotNull(update), checkNotNull(startModel))),
        checkNotNull(startModel),
        checkNotNull(startEffects),
        checkNotNull(effectHandler),
        checkNotNull(eventSource),
        checkNotNull(eventRunner),
        checkNotNull(effectRunner));
  }

  private MobiusLoop(
      EventProcessor.Factory<M, E, F> eventProcessorFactory,
      M startModel,
      Iterable<F> startEffects,
      Connectable<F, E> effectHandler,
      Connectable<M, E> eventSource,
      WorkRunner eventRunner,
      WorkRunner effectRunner) {

    Consumer<E> onEventReceived =
        new Consumer<E>() {
          @Override
          public void accept(E event) {
            eventProcessor.update(event);
          }
        };

    Consumer<F> onEffectReceived =
        new Consumer<F>() {
          @Override
          public void accept(F effect) {
            try {
              effectConsumer.accept(effect);
            } catch (Throwable t) {
              throw new ConnectionException(effect, t);
            }
          }
        };

    Consumer<M> onModelChanged =
        new Consumer<M>() {
          @Override
          public void accept(M model) {
            mostRecentModel = model;
            eventSourceModelConsumer.accept(model);
            for (Consumer<M> observer : modelObservers) {
              observer.accept(model);
            }
          }
        };

    this.safeOnEventReceived = new SafeConsumer<>(onEventReceived);
    this.safeOnEffectReceived = new SafeConsumer<>(onEffectReceived);

    this.eventDispatcher = new MessageDispatcher<>(eventRunner, safeOnEventReceived);
    this.effectDispatcher = new MessageDispatcher<>(effectRunner, safeOnEffectReceived);

    this.eventProcessor = eventProcessorFactory.create(effectDispatcher, onModelChanged);

    Consumer<E> eventConsumer =
        new Consumer<E>() {
          @Override
          public void accept(E event) {
            dispatchEvent(event);
          }
        };

    this.effectConsumer = effectHandler.connect(eventConsumer);
    this.eventSourceModelConsumer = eventSource.connect(eventConsumer);

    mostRecentModel = startModel;

    eventRunner.post(
        new Runnable() {
          @Override
          public void run() {
            onModelChanged.accept(startModel);
            for (F effect : startEffects) {
              effectDispatcher.accept(effect);
            }
          }
        });
  }

  public void dispatchEvent(E event) {
    if (disposed)
      throw new IllegalStateException(
          "This loop has already been disposed. You cannot dispatch events after disposal");
    eventDispatcher.accept(checkNotNull(event));
  }

  @Nullable
  public M getMostRecentModel() {
    return mostRecentModel;
  }

  /**
   * Add an observer of model changes to this loop. If {@link #getMostRecentModel()} is non-null,
   * the observer will immediately be notified of the most recent model. The observer will be
   * notified of future changes to the model until the loop or the returned {@link Disposable} is
   * disposed.
   *
   * @param observer a non-null observer of model changes
   * @return a {@link Disposable} that can be used to stop further notifications to the observer
   * @throws NullPointerException if the observer is null
   * @throws IllegalStateException if the loop has been disposed
   */
  public Disposable observe(final Consumer<M> observer) {
    if (disposed)
      throw new IllegalStateException(
          "This loop has already been disposed. You cannot observe a disposed loop");

    modelObservers.add(checkNotNull(observer));

    final M currentModel = mostRecentModel;
    if (currentModel != null) {
      // Start by emitting the most recently received model.
      observer.accept(currentModel);
    }

    return new Disposable() {
      @Override
      public void dispose() {
        modelObservers.remove(observer);
      }
    };
  }

  @Override
  public synchronized void dispose() {
    // Remove model observers so that they receive no further model changes.
    modelObservers.clear();

    // Disable the event and effect dispatchers. This will cause any further
    // events or effects posted to the dispatchers to be ignored and logged.
    safeOnEventReceived.dispose();
    safeOnEffectReceived.dispose();

    // Stop the event source and effect handler.
    eventSourceModelConsumer.dispose();
    effectConsumer.dispose();

    // Finally clean up the dispatchers that now no longer are needed.
    eventDispatcher.dispose();
    effectDispatcher.dispose();

    disposed = true;
  }

  /**
   * Defines a fluent API for configuring a {@link MobiusLoop}. Implementations must be immutable,
   * making them safe to share between threads.
   *
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   */
  public interface Builder<M, E, F> extends Factory<M, E, F> {

    /**
     * @deprecated Pass your initial effects to {@link #startFrom(Object, Set)} instead.
     * @return a new {@link Builder} with the supplied {@link Init}, and the same values as the
     *     current one for the other fields.
     */
    @Nonnull
    @Deprecated
    Builder<M, E, F> init(Init<M, F> init);

    /**
     * @return a new {@link Builder} with the supplied {@link EventSource}, and the same values as
     *     the current one for the other fields. NOTE: Invoking this method will replace the current
     *     {@link EventSource} with the supplied one. If you want to pass multiple event sources,
     *     please use {@link #eventSources(EventSource, EventSource[])}.
     */
    @Nonnull
    Builder<M, E, F> eventSource(EventSource<E> eventSource);

    /**
     * @return a new {@link Builder} with an {@link EventSource} that merges the supplied event
     *     sources, and the same values as the current one for the other fields.
     */
    @Nonnull
    Builder<M, E, F> eventSources(EventSource<E> eventSource, EventSource<E>... eventSources);

    /**
     * @return a new {@link Builder} with the supplied {@link Connectable<M,E>}, and the same values
     *     as the current one for the other fields. NOTE: Invoking this method will replace the
     *     current event source with the supplied one. If a loop has a {@link Connectable<M,E>} as
     *     its event source, it will connect to it and will invoke the {@link Connection<M>} accept
     *     method every time the model changes. This allows us to conditionally subscribe to
     *     different sources based on the current state. If you provide a regular {@link
     *     EventSource<E>}, it will be wrapped in a {@link Connectable} and that implementation will
     *     subscribe to that event source only once when the loop is initialized.
     */
    @Nonnull
    Builder<M, E, F> eventSource(Connectable<M, E> eventSource);

    /**
     * @return a new {@link Builder} with the supplied logger, and the same values as the current
     *     one for the other fields.
     */
    @Nonnull
    Builder<M, E, F> logger(Logger<M, E, F> logger);

    /**
     * @return a new {@link Builder} with the supplied event runner, and the same values as the
     *     current one for the other fields.
     */
    @Nonnull
    Builder<M, E, F> eventRunner(Producer<WorkRunner> eventRunner);

    /**
     * @return a new {@link Builder} with the supplied effect runner, and the same values as the
     *     current one for the other fields.
     */
    @Nonnull
    Builder<M, E, F> effectRunner(Producer<WorkRunner> effectRunner);
  }

  public interface Factory<M, E, F> {
    /**
     * Start a {@link MobiusLoop} using this factory.
     *
     * @param startModel the model that the loop should start from
     * @return the started {@link MobiusLoop}
     */
    MobiusLoop<M, E, F> startFrom(M startModel);

    /**
     * Start a {@link MobiusLoop} using this factory.
     *
     * @param startModel the model that the loop should start from
     * @param startEffects the effects that the loop should start with
     * @return the started {@link MobiusLoop}
     */
    MobiusLoop<M, E, F> startFrom(M startModel, Set<F> startEffects);
  }

  /**
   * Defines a controller that can be used to start and stop MobiusLoops.
   *
   * <p>If a loop is stopped and then started again, the new loop will continue from where the last
   * one left off.
   */
  public interface Controller<M, E> {
    /**
     * Indicates whether this controller is running.
     *
     * @return true if the controller is running
     */
    boolean isRunning();

    /**
     * Connect a view to this controller.
     *
     * <p>Must be called before {@link #start()}.
     *
     * <p>The {@link Connectable} will be given an event consumer, which the view should use to send
     * events to the MobiusLoop. The view should also return a {@link Connection} that accepts
     * models and renders them. Disposing the connection should make the view stop emitting events.
     *
     * <p>The view Connectable is guaranteed to only be connected once, so you don't have to check
     * for multiple connections or throw {@link ConnectionLimitExceededException}.
     *
     * @throws IllegalStateException if the loop is running or if the controller already is
     *     connected
     */
    void connect(Connectable<M, E> view);

    /**
     * Disconnect UI from this controller.
     *
     * @throws IllegalStateException if the loop is running or if there isn't anything to disconnect
     */
    void disconnect();

    /**
     * Start a MobiusLoop from the current model.
     *
     * @throws IllegalStateException if the loop already is running or no view has been connected
     */
    void start();

    /**
     * Stop the currently running MobiusLoop.
     *
     * <p>When the loop is stopped, the last model of the loop will be remembered and used as the
     * first model the next time the loop is started.
     *
     * @throws IllegalStateException if the loop isn't running
     */
    void stop();

    /**
     * Replace which model the controller should start from.
     *
     * @param model the model with the state the controller should start from
     * @throws IllegalStateException if the loop is running
     */
    void replaceModel(M model);

    /**
     * Get the current model of the loop that this controller is running, or the most recent model
     * if it's not running.
     *
     * @return a model with the state of the controller
     */
    @Nonnull
    M getModel();
  }

  /** Interface for logging init and update calls. */
  public interface Logger<M, E, F> {
    /**
     * Called right before the {@link Init#init(Object)} function is called.
     *
     * <p>This method mustn't block, as it'll hinder the loop from running. It will be called on the
     * same thread as the init function.
     *
     * @param model the model that will be passed to the init function
     */
    void beforeInit(M model);

    /**
     * Called right after the {@link Init#init(Object)} function is called.
     *
     * <p>This method mustn't block, as it'll hinder the loop from running. It will be called on the
     * same thread as the init function.
     *
     * @param model the model that was passed to init
     * @param result the {@link First} that init returned
     */
    void afterInit(M model, First<M, F> result);

    /**
     * Called if the {@link Init#init(Object)} invocation throws an exception. This is a programmer
     * error; Mobius is in an undefined state if it happens.
     *
     * @param model the model object that led to the exception
     * @param exception the thrown exception
     */
    void exceptionDuringInit(M model, Throwable exception);

    /**
     * Called right before the {@link Update#update(Object, Object)} function is called.
     *
     * <p>This method mustn't block, as it'll hinder the loop from running. It will be called on the
     * same thread as the update function.
     *
     * @param model the model that will be passed to the update function
     * @param event the event that will be passed to the update function
     */
    void beforeUpdate(M model, E event);

    /**
     * Called right after the {@link Update#update(Object, Object)} function is called.
     *
     * <p>This method mustn't block, as it'll hinder the loop from running. It will be called on the
     * same thread as the update function.
     *
     * @param model the model that was passed to update
     * @param event the event that was passed to update
     * @param result the {@link Next} that update returned
     */
    void afterUpdate(M model, E event, Next<M, F> result);

    /**
     * Called if the {@link Update#update(Object, Object)} invocation throws an exception. This is a
     * programmer error; Mobius is in an undefined state if it happens.
     *
     * @param model the model object that led to the exception
     * @param exception the thrown exception
     */
    void exceptionDuringUpdate(M model, E event, Throwable exception);
  }
}
