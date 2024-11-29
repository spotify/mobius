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
