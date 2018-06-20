[![Maven Central](https://img.shields.io/maven-central/v/com.spotify.mobius/mobius-core.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.spotify.mobius%22)
[![Build Status](https://travis-ci.org/spotify/mobius.svg?branch=master)](https://travis-ci.org/spotify/mobius)
[![Code Coverage](https://codecov.io/gh/spotify/mobius/branch/master/graph/badge.svg)](https://codecov.io/gh/spotify/mobius)
[![License](https://img.shields.io/github/license/spotify/mobius.svg)](LICENSE)
[![Join the chat at https://gitter.im/spotify/mobius](https://badges.gitter.im/spotify/mobius.svg)](https://gitter.im/spotify/mobius)

# Mobius

Mobius is a functional reactive framework for managing state evolution and side-effects, with add-ons for connecting to Android UIs and RxJava Observables. It emphasizes separation of concerns, testability, and isolating stateful parts of the code.

To learn more, see the [wiki](https://github.com/spotify/mobius/wiki) for a user guide. To see Mobius in action, check out the [sample TODO](https://github.com/spotify/mobius-android-sample) app based on the app from [Android Architecture Blueprints](https://github.com/googlesamples/android-architecture). You can also
watch a [talk from Android @Scale introducing Mobius](https://www.facebook.com/atscaleevents/videos/2025571921049235/).

## Status

Mobius is in Beta status, meaning it is used in production in Spotify Android applications, but
we may keep making changes relatively quickly.

Mobius is currently built for Java 7 (because Java 8 is not fully supported on all versions of Android), hence the duplication of some concepts defined in `java.util.function` (see `com.spotify.mobius.functions`).

When using Mobius, we do however recommend using Java 8 or later, primarily because of the improved type inference and because using lambdas greatly improves readability and conciseness of code.

## Using it in your project

The latest version of Mobius is available through Maven Central (LATEST_RELEASE below is ![latest not found](https://img.shields.io/maven-central/v/com.spotify.mobius/mobius-core.svg)):

```groovy
implementation 'com.spotify.mobius:mobius-core:LATEST_RELEASE'
testImplementation 'com.spotify.mobius:mobius-test:LATEST_RELEASE'

implementation 'com.spotify.mobius:mobius-rx:LATEST_RELEASE'       // only for RxJava 1 support
implementation 'com.spotify.mobius:mobius-rx2:LATEST_RELEASE'      // only for RxJava 2 support
implementation 'com.spotify.mobius:mobius-android:LATEST_RELEASE'  // only for Android support
implementation 'com.spotify.mobius:mobius-extras:LATEST_RELEASE'   // utilities for common patterns
```

## Building

### Formatting

We're using Google's auto-formatter to format the code. The build pipeline is set up to fail builds that aren't correctly formatted. To ensure correct formatting, run

```bash
./gradlew format
```

## Code of Conduct

This project adheres to the [Open Code of Conduct][code-of-conduct]. By participating, you are expected to honor this code.

[code-of-conduct]: https://github.com/spotify/code-of-conduct/blob/master/code-of-conduct.md
