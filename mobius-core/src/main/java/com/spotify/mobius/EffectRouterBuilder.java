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

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;

/** TODO: document! */
public interface EffectRouterBuilder<F, E> {

  <G extends F> EffectRouterBuilder<F, E> addRunnable(Class<G> klazz, Runnable action);

  <G extends F> EffectRouterBuilder<F, E> addConsumer(Class<G> klazz, Consumer<G> consumer);

  <G extends F> EffectRouterBuilder<F, E> addFunction(Class<G> klazz, Function<G, E> function);

  <G extends F> EffectRouterBuilder<F, E> addConnectable(
      Class<G> klazz, Connectable<G, E> connectable);

  Connectable<F, E> build();
}
