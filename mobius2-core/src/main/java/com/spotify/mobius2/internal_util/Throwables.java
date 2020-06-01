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
package com.spotify.mobius2.internal_util;

/** Utilities for working with throwables. */
public final class Throwables {
  private Throwables() {
    // prevent instantiation
  }

  public static RuntimeException propagate(Exception e) {
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    }

    throw new RuntimeException(e);
  }
}
