package com.spotify.mobius.runners;

public class DefaultWorkRunners {

    public static WorkRunner defaultEffectRunner() {
        return WorkRunners.cachedThreadPool();
    }

    public static WorkRunner defaultEventRunner() {
        return WorkRunners.singleThread();
    }
}
