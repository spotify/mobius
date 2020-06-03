/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
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

import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class ControllerStateRunning<M, E, F> extends ControllerStateBase<M, E> {
  @Nonnull private final ControllerActions<M, E> actions;
  @Nonnull private final Connection<M> renderer;
  @Nonnull private final MobiusLoop<M, E, F> loop;
  @Nonnull private final M startModel;

  ControllerStateRunning(
      ControllerActions<M, E> actions,
      Connection<M> renderer,
      MobiusLoop.Factory<M, E, F> loopFactory,
      M modelToStartFrom,
      @Nullable Init<M, F> init) {

    this.actions = actions;
    this.renderer = renderer;

    if (init != null) {
      First<M, F> first = init.init(modelToStartFrom);
      this.loop = loopFactory.startFrom(first.model(), first.effects());
      this.startModel = first.model();
    } else {
      this.loop = loopFactory.startFrom(modelToStartFrom);
      this.startModel = modelToStartFrom;
    }
  }

  void start() {
    loop.observe(
        new Consumer<M>() {
          @Override
          public void accept(M model) {
            actions.postUpdateView(model);
          }
        });
  }

  @Override
  protected String getStateName() {
    return "running";
  }

  @Override
  public boolean isRunning() {
    return true;
  }

  @Override
  public void onDispatchEvent(E event) {
    loop.dispatchEvent(event);
  }

  @Override
  public void onUpdateView(M model) {
    renderer.accept(model);
  }

  @Override
  public void onStop() {
    loop.dispose();
    M mostRecentModel = loop.getMostRecentModel();
    actions.goToStateCreated(renderer, mostRecentModel);
  }

  @Nonnull
  @Override
  public M onGetModel() {
    M model = loop.getMostRecentModel();
    return model != null ? model : startModel;
  }
}
