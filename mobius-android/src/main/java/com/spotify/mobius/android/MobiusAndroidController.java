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
package com.spotify.mobius.android;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import android.os.Bundle;
import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.WorkRunner;
import javax.annotation.Nullable;

class MobiusAndroidController<M, E, F> implements MobiusController<M, E>, ControllerActions<M, E> {

  private final MobiusLoop.Factory<M, E, F> loopFactory;
  private final ModelSaveRestore<M> modelSaveRestore;
  private final WorkRunner mainThreadRunner;

  private ControllerStateBase<M, E> currentState;

  MobiusAndroidController(
      MobiusLoop.Factory<M, E, F> loopFactory,
      ModelSaveRestore<M> modelSaveRestore,
      WorkRunner mainThreadRunner) {

    this.loopFactory = checkNotNull(loopFactory);
    this.modelSaveRestore = checkNotNull(modelSaveRestore);
    this.mainThreadRunner = checkNotNull(mainThreadRunner);
    goToStateInit(null);
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
  public synchronized void restoreState(@Nullable Bundle in) {
    if (in == null) {
      return;
    }

    currentState.onRestoreState(in);
  }

  @Override
  public synchronized void saveState(@Nullable Bundle out) {
    if (out == null) {
      return;
    }

    currentState.onSaveState(out);
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
  public synchronized void goToStateInit(@Nullable M nextModelToStartFrom) {
    currentState = new ControllerStateInit<>(this, modelSaveRestore, nextModelToStartFrom);
  }

  @Override
  public synchronized void goToStateCreated(
      Connection<M> renderer, @Nullable M nextModelToStartFrom) {
    currentState =
        new ControllerStateCreated<M, E, F>(this, modelSaveRestore, renderer, nextModelToStartFrom);
  }

  @Override
  public void goToStateCreated(Connectable<M, E> view, @Nullable M nextModelToStartFrom) {

    SafeConnectable<M, E> safeModelHandler = new SafeConnectable<>(checkNotNull(view));

    Connection<M> modelConnection =
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
    ControllerStateRunning<M, E, F> stateRunning =
        new ControllerStateRunning<>(this, renderer, loopFactory, nextModelToStartFrom);

    currentState = stateRunning;

    stateRunning.start();
  }
}
