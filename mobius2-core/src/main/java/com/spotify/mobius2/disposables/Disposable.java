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
package com.spotify.mobius2.disposables;

/**
 * A {@code Disposable} is an object that may be holding on to references or resources that need to
 * be released when the object is no longer needed. The dispose method is invoked to release
 * resources that the object is holding.
 */
public interface Disposable {
  /**
   * Dispose of all resources associated with this object.
   *
   * <p>The object will no longer be valid after dispose has been called, and any further calls to
   * dispose won't have any effect.
   */
  void dispose();
}
