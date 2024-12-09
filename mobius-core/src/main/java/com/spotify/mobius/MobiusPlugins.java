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

import com.spotify.mobius.functions.Producer;
import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.runners.WorkRunners;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobiusPlugins {

  @Nullable private static Producer<WorkRunner> EFFECT_RUNNER_OVERRIDE_PRODUCER;
  @Nullable private static Producer<WorkRunner> EVENT_RUNNER_OVERRIDE_PRODUCER;

  /**
   * Sets the effect runner producer that will be used in {@link MobiusLoop} when effectRunner was
   * not provided to {@link MobiusLoop.Builder}.
   *
   * @param defaultEffectRunnerProducer the {@link WorkRunner} producer to use as the default effect
   *     runner. A new instance needs to be provided each time the producer is called.
   */
  public static void setDefaultEffectRunner(Producer<WorkRunner> defaultEffectRunnerProducer) {
    EFFECT_RUNNER_OVERRIDE_PRODUCER = defaultEffectRunnerProducer;
  }

  /**
   * Sets the event runner producer that will be used in {@link MobiusLoop} when eventRunner was not
   * provided to {@link MobiusLoop.Builder}.
   *
   * @param defaultEventRunnerProducer the {@link WorkRunner} producer to use as the default event
   *     runner. A new instance needs to be provided each time the producer is called.
   */
  public static void setDefaultEventRunner(Producer<WorkRunner> defaultEventRunnerProducer) {
    EVENT_RUNNER_OVERRIDE_PRODUCER = defaultEventRunnerProducer;
  }

  @Nonnull
  static WorkRunner defaultEffectRunner() {
    if (EFFECT_RUNNER_OVERRIDE_PRODUCER != null) {
      return EFFECT_RUNNER_OVERRIDE_PRODUCER.get();
    }
    return WorkRunners.cachedThreadPool();
  }

  @Nonnull
  static WorkRunner defaultEventRunner() {
    if (EVENT_RUNNER_OVERRIDE_PRODUCER != null) {
      return EVENT_RUNNER_OVERRIDE_PRODUCER.get();
    }
    return WorkRunners.singleThread();
  }
}
