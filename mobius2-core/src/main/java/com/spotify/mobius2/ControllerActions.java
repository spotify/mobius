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

import javax.annotation.Nullable;

interface ControllerActions<M, E> {

  void postUpdateView(M model);

  void goToStateInit(M nextModelToStartFrom);

  void goToStateCreated(
      com.spotify.mobius2.Connection<M> renderer, @Nullable M nextModelToStartFrom);

  void goToStateCreated(Connectable<M, E> view, M nextModelToStartFrom);

  void goToStateRunning(Connection<M> renderer, M nextModelToStartFrom);
}
