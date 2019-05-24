package com.spotify.mobius.testdomain;

public class SafeEffect implements TestEffect {

  public final String id;

  public SafeEffect(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "effect" + id;
  }
}
