package com.spotify.mobius.testdomain;

public class EventWithCrashingEffect extends TestEvent {

  public EventWithCrashingEffect() {
    super("crash!");
  }
}
