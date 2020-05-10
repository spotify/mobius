![Mobius Logo](https://github.com/spotify/mobius/wiki/mobius-logo.png)

[![Maven Central](https://img.shields.io/maven-central/v/com.spotify.mobius/mobius-core.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.spotify.mobius%22)
[![Build Status](https://travis-ci.org/spotify/mobius.svg?branch=master)](https://travis-ci.org/spotify/mobius)
[![Code Coverage](https://codecov.io/gh/spotify/mobius/branch/master/graph/badge.svg)](https://codecov.io/gh/spotify/mobius)
[![License](https://img.shields.io/github/license/spotify/mobius.svg)](LICENSE)
[![Join the chat at https://gitter.im/spotify/mobius](https://badges.gitter.im/spotify/mobius.svg)](https://gitter.im/spotify/mobius)

Mobius is a functional reactive framework for managing state evolution and side-effects, with add-ons for connecting to Android UIs and RxJava Observables. It emphasizes separation of concerns, testability, and isolating stateful parts of the code.

To learn more, see the [wiki](https://github.com/spotify/mobius/wiki) for a user guide. To see Mobius in action, check out the [sample TODO](https://github.com/spotify/mobius-android-sample) app based on the app from [Android Architecture Blueprints](https://github.com/googlesamples/android-architecture). You can also
watch a [talk from Android @Scale introducing Mobius](https://www.facebook.com/atscaleevents/videos/2025571921049235/).

## Status

Mobius is in Production status, meaning it is used in production in Spotify Android applications, and that we consider the APIs to be stable and the implementation bug-free. We will not make backwards-compatibility-breaking changes.

Mobius is currently built for Java 7 (because Java 8 is not fully supported on all versions of Android), hence the duplication of some concepts defined in `java.util.function` (see `com.spotify.mobius.functions`).

When using Mobius, we recommend using Kotlin or Java 8 or later, primarily because of the improved type inference and because using lambdas greatly improves readability and conciseness of code.

## Using it in your project

The latest version of Mobius is available through Maven Central (LATEST_RELEASE below is ![latest not found](https://img.shields.io/maven-central/v/com.spotify.mobius/mobius-core.svg)):

```groovy
implementation 'com.spotify.mobius:mobius-core:LATEST_RELEASE'
testImplementation 'com.spotify.mobius:mobius-test:LATEST_RELEASE'

implementation 'com.spotify.mobius:mobius-rx:LATEST_RELEASE'       // only for RxJava 1 support
implementation 'com.spotify.mobius:mobius-rx2:LATEST_RELEASE'      // only for RxJava 2 support
implementation 'com.spotify.mobius:mobius-rx3:LATEST_RELEASE'      // only for RxJava 3 support
implementation 'com.spotify.mobius:mobius-android:LATEST_RELEASE'  // only for Android support
implementation 'com.spotify.mobius:mobius-extras:LATEST_RELEASE'   // utilities for common patterns
```

## Mobius in Action - Building a Counter

The goal of Mobius is to give you better control over your application state. You can think of your state as a snapshot of all the current values of the variables in your application. In Mobius, we encapsulate all of the state in a data-structure which we call the *Model*.

The *Model* can be represented by whatever type you like. In this example we'll be building a simple counter, so all of our state can be contained in an `Integer`:

Mobius does not let you manipulate the state directly. In order to change the state, you have to send the framework messages saying what you want to do. We call these messages *Events*. In our case, we'll want to increment and decrement our counter. Let's use an `enum` to define these cases:
```java
enum CounterEvent {
  INCREMENT,
  DECREMENT,
}
```

Now that we have a *Model* and some *Event*s, we'll need to give Mobius a set of rules which it can use to update the state on our behalf. We do this by giving the framework a function which will be sequentially called with every incoming *Event* and the most recent *Model*, in order to generate the next *Model*:
```java
class CounterLogic {
  static Integer update(Integer model, CounterEvent event) {
    switch (event) {
      case INCREMENT: return model + 1;
      case DECREMENT: return model - 1;
    }
  }
}
```

With these building blocks, we can start to think about our applications as transitions between discrete states in response to events. But we believe there still one piece missing from the puzzle - namely the side-effects which are associated with moving between states. For instance, pressing a "refresh" button might put our application into a "loading" state, with the side-effect of also fetching the latest data from our backend.

In Mobius, we aptly call these side-effects *Effect*s. In the case of our counter, let's say that when the user tries to decrement below 0, we play a sound effect instead. Let's create an `enum` that represents all the possible effects (which in this case is only one):
```java
enum CounterEffect {
  PLAY_SOUND,
}
```

We'll now need to augment our `update` function to also return a set of effects associated with certain state transitions. To do this we'll implement the `Update` interface like so:

```java
class CounterLogic implements Update<Integer, CounterEvent, CounterEffect> {
  public Next<Integer, CounterEffect> update(Integer model, CounterEvent event) {
    switch (event) {
      case INCREMENT:
        return next(model + 1);
      case DECREMENT:
        if (model == 0) {
          Set<CounterEffect> soundEffect = effects(CounterEffect.PLAY_SOUND);
          return dispatch(soundEffect);
        }
        return next(model - 1);
    }
    throw new IllegalStateException("Unhandled event: " + event);
  }
}
```

Mobius sends each of the effects you return in any state transition to something called an *Effect Handler*. Let's make one of those now by implementing the `Connectable` interface:
```java
class CounterEffectHandler implements Connectable<CounterEffect, CounterEvent> {
  public Connection<CounterEffect> connect(Consumer<CounterEvent> output) {
    return new Connection<CounterEffect>() {
      @Override
      public void accept(CounterEffect effect) {
        if (effect == CounterEffect.PLAY_SOUND) {
          Toolkit.getDefaultToolkit().beep();
        }
      }

      @Override
      public void dispose() {}
    };
  }
}
```

Now that we have all the pieces in place, let's tie it all together:
```java
public static void main(String[] args) {
  // Let's make a Mobius Loop
  MobiusLoop<Integer, CounterEvent, CounterEffect> loop = Mobius
      .loop(new CounterLogic(), new CounterEffectHandler())
      .startFrom(0);

  // And start using our loop
  loop.dispatchEvent(CounterEvent.INCREMENT); // Model is now 1
  loop.dispatchEvent(CounterEvent.DECREMENT); // Model is now 0
  loop.dispatchEvent(CounterEvent.DECREMENT); // Sound effect plays! Model is still 0
}
```

This covers the fundamentals of Mobius. To learn more, head on over to our [wiki](/../../wiki).

## Building

### Formatting

We're using Google's auto-formatter to format the code. The build pipeline is set up to fail builds that aren't correctly formatted. To ensure correct formatting, run

```bash
./gradlew format
```

## Code of Conduct

This project adheres to the [Open Code of Conduct][code-of-conduct]. By participating, you are expected to honor this code.

[code-of-conduct]: https://github.com/spotify/code-of-conduct/blob/master/code-of-conduct.md
