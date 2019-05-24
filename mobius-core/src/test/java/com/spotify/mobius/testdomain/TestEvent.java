package com.spotify.mobius.testdomain;

public class TestEvent {

  private final String name;

  public TestEvent(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
