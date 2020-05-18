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
package com.spotify.mobius2;

import static com.spotify.mobius2.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius2.functions.Consumer;
import com.spotify.mobius2.runners.WorkRunner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MobiusLoopController<M, E, F>
    implements com.spotify.mobius2.MobiusLoop.Controller<M, E>,
        com.spotify.mobius2.ControllerActions<M, E> {

  private final com.spotify.mobius2.MobiusLoop.Factory<M, E, F> loopFactory;
  private final M defaultModel;
  @Nullable private final com.spotify.mobius2.Init<M, F> init;
  private final WorkRunner mainThreadRunner;

  private com.spotify.mobius2.ControllerStateBase<M, E> currentState;

  MobiusLoopController(
      MobiusLoop.Factory<M, E, F> loopFactory,
      M defaultModel,
      @Nullable Init<M, F> init,
      WorkRunner mainThreadRunner) {

    this.loopFactory = checkNotNull(loopFactory);
    this.defaultModel = checkNotNull(defaultModel);
    this.init = init;
    this.mainThreadRunner = checkNotNull(mainThreadRunner);
    goToStateInit(defaultModel);
  }

  @Override
  public synchronized boolean isRunning() {
    return currentState.isRunning();
  }

  private synchronized void dispatchEvent(E event) {
    currentState.onDispatchEvent(event);
  }

  private synchronized void updateView(M model) {
    currentState.onUpdateView(model);
  }

  @Override
  public synchronized void connect(Connectable<M, E> view) {
    currentState.onConnect(checkNotNull(view));
  }

  @Override
  public synchronized void disconnect() {
    currentState.onDisconnect();
  }

  @Override
  public synchronized void start() {
    currentState.onStart();
  }

  @Override
  public synchronized void stop() {
    currentState.onStop();
  }

  @Override
  public synchronized void replaceModel(M model) {
    checkNotNull(model);
    currentState.onReplaceModel(model);
  }

  @Override
  @Nonnull
  public synchronized M getModel() {
    return currentState.onGetModel();
  }

  public void postUpdateView(final M model) {
    mainThreadRunner.post(
        new Runnable() {
          @Override
          public void run() {
            updateView(model);
          }
        });
  }

  @Override
  public synchronized void goToStateInit(M nextModelToStartFrom) {
    currentState = new ControllerStateInit<>(this, nextModelToStartFrom);
  }

  @Override
  public synchronized void goToStateCreated(
      com.spotify.mobius2.Connection<M> renderer, @Nullable M nextModelToStartFrom) {

    if (nextModelToStartFrom == null) {
      nextModelToStartFrom = defaultModel;
    }

    currentState = new ControllerStateCreated<M, E, F>(this, renderer, nextModelToStartFrom);
  }

  @Override
  public void goToStateCreated(Connectable<M, E> view, M nextModelToStartFrom) {

    DiscardAfterDisposeConnectable<M, E> safeModelHandler =
        new DiscardAfterDisposeConnectable<>(checkNotNull(view));

    com.spotify.mobius2.Connection<M> modelConnection =
        safeModelHandler.connect(
            new Consumer<E>() {
              @Override
              public void accept(E event) {
                dispatchEvent(event);
              }
            });

    goToStateCreated(checkNotNull(modelConnection), nextModelToStartFrom);
  }

  @Override
  public synchronized void goToStateRunning(Connection<M> renderer, M nextModelToStartFrom) {
    com.spotify.mobius2.ControllerStateRunning<M, E, F> stateRunning =
        new com.spotify.mobius2.ControllerStateRunning<>(
            this, renderer, loopFactory, nextModelToStartFrom, init);

    currentState = stateRunning;

    stateRunning.start();
  }
}
