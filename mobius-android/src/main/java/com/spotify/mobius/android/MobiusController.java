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
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import javax.annotation.Nullable;

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
   * <p>The {@link Connectable} will given an event consumer, which the view should use to send
   * events to the MobiusLoop. The view should also return a {@link Connection} that accepts models
   * and renders them. Disposing the connection should make the view stop emitting events.
   *
   * <p>The view Connectable is guaranteed to only be connected once, so you don't have to check for
   * multiple connections or throw {@link ConnectionLimitExceededException}.
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
   * Invoke this method if you're restoring state from a bundle.
   *
   * @param in the bundle that contains the serialized state
   */
  void restoreState(@Nullable Bundle in);

  /**
   * Invoke this method when you wish to save the current state for later restoration.
   *
   * @param out the bundle to save the state in
   */
  void saveState(@Nullable Bundle out);
}
