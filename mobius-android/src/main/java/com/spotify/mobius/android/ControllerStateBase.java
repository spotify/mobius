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
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note: synchronization has to be provided externally, states do not protect themselves from issues
 * related state switching. Use ControllerActions to interact with MobiusControllerActions for any
 * asynchronous action and never call one on-method from another directly.
 */
abstract class ControllerStateBase<M, E> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ControllerStateBase.class);

  protected abstract String getStateName();

  public boolean isRunning() {
    return false;
  }

  public void onConnect(Connectable<M, E> view) {
    throw new IllegalStateException(
        String.format("cannot call connect when in the %s state", getStateName()));
  }

  public void onDisconnect() {
    throw new IllegalStateException(
        String.format("cannot call disconnect when in the %s state", getStateName()));
  }

  public void onStart() {
    throw new IllegalStateException(
        String.format("cannot call start when in the %s state", getStateName()));
  }

  public void onStop() {
    throw new IllegalStateException(
        String.format("cannot call stop when in the %s state", getStateName()));
  }

  public void onRestoreState(@Nullable Bundle in) {
    throw new IllegalStateException(
        String.format("cannot call restoreState when in the %s state", getStateName()));
  }

  public void onSaveState(Bundle out) {
    throw new IllegalStateException(
        String.format("cannot call saveState when in the %s state", getStateName()));
  }

  public void onDispatchEvent(E event) {
    LOGGER.debug(
        "Dropping event that was dispatched when the program was in the {} state: {}",
        getStateName(),
        event);
  }

  public void onUpdateView(M model) {
    LOGGER.debug(
        "Dropping model that was dispatched when the program was in the {} state: {}",
        getStateName(),
        model);
  }
}
