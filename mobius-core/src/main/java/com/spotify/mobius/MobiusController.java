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

import javax.annotation.Nonnull;

public interface MobiusController<M, E> {
  /**
   * Indicates whether this controller has been started.
   *
   * @return true if the controller has been started
   */
  boolean isRunning();

  /**
   * Connect a view to this controller.
   *
   * <p>Must be called before {@link #start()}.
   *
   * <p>The {@link com.spotify.mobius.Connectable} will given an event consumer, which the view
   * should use to send events to the MobiusLoop. The view should also return a {@link
   * com.spotify.mobius.Connection} that accepts models and renders them. Disposing the connection
   * should make the view stop emitting events.
   *
   * <p>The view Connectable is guaranteed to only be connected once, so you don't have to check for
   * multiple connections or throw {@link com.spotify.mobius.ConnectionLimitExceededException}.
   */
  void connect(Connectable<M, E> view);

  /** Disconnect UI from this controller. Can only be called if connected but not started. */
  void disconnect();

  /**
   * Call this method to kick things off. This will start your Mobius loop from either the default
   * model or a restored model if one exists.
   */
  void start();

  /**
   * Call this method to stop your mobius loop from running. Typically called when UI is no longer
   * present.
   */
  void stop();

  /**
   * Invoke this method when you want to restore the controller to a specific state.
   *
   * <p>May only be called when the controller isn't running.
   *
   * @param model the model with the state the controller should be restored to
   */
  void restoreState(M model);

  /**
   * Invoke this method when you wish to save the current state of the controller.
   *
   * <p>May only be called when the controller isn't running.
   *
   * @return a model with the state of the controller
   */
  @Nonnull
  M saveState();
}
