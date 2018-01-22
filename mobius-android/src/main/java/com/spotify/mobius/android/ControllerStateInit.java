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
import com.spotify.mobius.Connectable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ControllerStateInit<M, E, F> extends ControllerStateBase<M, E> {

  @Nonnull private final ControllerActions<M, E> actions;
  @Nonnull private final ModelSaveRestore<M> modelSaveRestore;

  @Nullable private M nextModelToStartFrom;

  ControllerStateInit(
      ControllerActions<M, E> actions,
      ModelSaveRestore<M> modelSaveRestore,
      @Nullable M nextModelToStartFrom) {

    this.actions = actions;
    this.modelSaveRestore = modelSaveRestore;
    this.nextModelToStartFrom = nextModelToStartFrom;
  }

  @Override
  protected String getStateName() {
    return "init";
  }

  @Override
  public void onConnect(Connectable<M, E> view) {
    actions.goToStateCreated(view, nextModelToStartFrom);
  }

  @Override
  public void onRestoreState(@Nullable Bundle in) {
    if (in != null) {
      nextModelToStartFrom = modelSaveRestore.restoreModel(in);
    }
  }

  @Override
  public void onSaveState(@Nullable Bundle out) {
    if (nextModelToStartFrom != null && out != null) {
      modelSaveRestore.saveModel(nextModelToStartFrom, out);
    }
  }
}
