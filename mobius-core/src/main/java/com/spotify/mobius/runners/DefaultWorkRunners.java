package com.spotify.mobius.runners;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultWorkRunners {

    @Nullable
    private static WorkRunner EFFECT_RUNNER_OVERRIDE;
    @Nullable
    private static WorkRunner EVENT_RUNNER_OVERRIDE;

    public static void setDefaultEffectRunner(WorkRunner defaultEffectRunner) {
        EFFECT_RUNNER_OVERRIDE = defaultEffectRunner;
    }

    public static void setDefaultEventRunner(WorkRunner defaultEventRunner) {
        EVENT_RUNNER_OVERRIDE = defaultEventRunner;
    }

    @Nonnull
    public static WorkRunner defaultEffectRunner() {
        if (EFFECT_RUNNER_OVERRIDE != null) {
            return EFFECT_RUNNER_OVERRIDE;
        }
        return WorkRunners.cachedThreadPool();
    }

    @Nonnull
    public static WorkRunner defaultEventRunner() {
        if (EVENT_RUNNER_OVERRIDE != null) {
            return EVENT_RUNNER_OVERRIDE;
        }
        return WorkRunners.singleThread();
    }
}
