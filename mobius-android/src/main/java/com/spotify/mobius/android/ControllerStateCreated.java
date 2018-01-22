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

import android.os.Bundle;
import com.spotify.mobius.Connection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ControllerStateCreated<M, E, F> extends ControllerStateBase<M, E> {
  @Nonnull private final ControllerActions<M, E> actions;
  @Nonnull private final ModelSaveRestore<M> modelSaveRestore;
  @Nonnull private final Connection<M> renderer;

  @Nonnull private M nextModelToStartFrom;

  ControllerStateCreated(
      ControllerActions<M, E> actions,
      ModelSaveRestore<M> modelSaveRestore,
      Connection<M> renderer,
      @Nullable M nextModelToStartFrom) {

    this.actions = actions;
    this.modelSaveRestore = modelSaveRestore;
    this.renderer = renderer;

    if (nextModelToStartFrom != null) {
      this.nextModelToStartFrom = nextModelToStartFrom;
    } else {
      this.nextModelToStartFrom = modelSaveRestore.getDefaultModel();
    }
  }

  @Override
  protected String getStateName() {
    return "created";
  }

  @Override
  public void onDisconnect() {
    renderer.dispose();
    actions.goToStateInit(nextModelToStartFrom);
  }

  @Override
  public void onStart() {
    actions.goToStateRunning(renderer, nextModelToStartFrom);
  }

  @Override
  public void onRestoreState(@Nullable Bundle in) {
    if (in != null) {
      M model = modelSaveRestore.restoreModel(in);
      if (model != null) {
        nextModelToStartFrom = model;
      }
    }
  }

  @Override
  public void onSaveState(Bundle out) {
    modelSaveRestore.saveModel(nextModelToStartFrom, out);
  }
}
