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

import com.spotify.mobius2.disposables.Disposable;
import com.spotify.mobius2.functions.Consumer;

/**
 * Handle for a connection created by {@link Connectable}.
 *
 * <p>Used for sending values to the connection and to dispose of it and all resources associated
 * with it.
 */
public interface Connection<I> extends Disposable, Consumer<I> {

  /**
   * Send a value to this connection. Implementations may receive values from different threads and
   * are thus expected to be thread-safe.
   *
   * @param value the value that should be sent to the connection
   */
  void accept(I value);

  /**
   * Disconnect this connection and dispose of all resources associated with it.
   *
   * <p>The connection will no longer be valid after dispose has been called. No further values will
   * be accepted, and any repeated calls to dispose should be ignored.
   */
  void dispose();
}
