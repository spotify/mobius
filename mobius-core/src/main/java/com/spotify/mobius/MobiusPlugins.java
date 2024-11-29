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

import com.spotify.mobius.runners.WorkRunner;
import com.spotify.mobius.runners.WorkRunners;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobiusPlugins {

  @Nullable private static WorkRunner EFFECT_RUNNER_OVERRIDE;
  @Nullable private static WorkRunner EVENT_RUNNER_OVERRIDE;

  /**
   * Sets the effect runner that will be used in {@link MobiusLoop} when effectRunner was not
   * provided to {@link MobiusLoop.Builder}.
   *
   * @param defaultEffectRunner the {@link WorkRunner} to use as the default effect runner
   */
  public static void setDefaultEffectRunner(WorkRunner defaultEffectRunner) {
    EFFECT_RUNNER_OVERRIDE = defaultEffectRunner;
  }

  /**
   * Sets the event runner that will be used in {@link MobiusLoop} when eventRunner was not provided
   * to {@link MobiusLoop.Builder}.
   *
   * @param defaultEventRunner the {@link WorkRunner} to use as the default event runner
   */
  public static void setDefaultEventRunner(WorkRunner defaultEventRunner) {
    EVENT_RUNNER_OVERRIDE = defaultEventRunner;
  }

  @Nonnull
  static WorkRunner defaultEffectRunner() {
    if (EFFECT_RUNNER_OVERRIDE != null) {
      return EFFECT_RUNNER_OVERRIDE;
    }
    return WorkRunners.cachedThreadPool();
  }

  @Nonnull
  static WorkRunner defaultEventRunner() {
    if (EVENT_RUNNER_OVERRIDE != null) {
      return EVENT_RUNNER_OVERRIDE;
    }
    return WorkRunners.singleThread();
  }
}
