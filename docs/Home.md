![](https://github.com/spotify/mobius/wiki/mobius-logo.png)

Mobius is a functional reactive framework for managing state evolution and side-effects, with add-ons for connecting to Android UIs and RxJava Observables. It emphasizes separation of concerns, testability, and isolating stateful parts of the code.

To learn more, start with reading the [Concepts](Concepts) overview.

## Modules

The latest version of Mobius is available through Maven Central (LATEST_RELEASE below is
![latest not found](https://img.shields.io/maven-central/v/com.spotify.mobius/mobius-core.svg)):
 
    com.spotify.mobius:mobius-core:LATEST_RELEASE
    com.spotify.mobius:mobius-test:LATEST_RELEASE
    com.spotify.mobius:mobius-extras:LATEST_RELEASE
    com.spotify.mobius:mobius-rx:LATEST_RELEASE
    com.spotify.mobius:mobius-rx2:LATEST_RELEASE
    com.spotify.mobius:mobius-android:LATEST_RELEASE

### mobius-core [![Javadocs](http://www.javadoc.io/badge/com.spotify.mobius/mobius-core.svg?color=blue)](http://www.javadoc.io/doc/com.spotify.mobius/mobius-core)
This is the core of Mobius, which all other modules depend on. It is a pure Java library that is completely self-contained. This is the only module that you need when using Mobius, because the others are optional extensions to the core.

### mobius-test [![Javadocs](http://www.javadoc.io/badge/com.spotify.mobius/mobius-test.svg?color=blue)](http://www.javadoc.io/doc/com.spotify.mobius/mobius-test)
The test module contains utilities that help you write tests for Mobius applications. It should only be used as a test dependency.

### mobius-extras [![Javadocs](http://www.javadoc.io/badge/com.spotify.mobius/mobius-extras.svg?color=blue)](http://www.javadoc.io/doc/com.spotify.mobius/mobius-extras)
The extras module contains utilities and classes that help reducing boilerplate for some more advanced usage patterns (for example, nested update functions).

### mobius-rx [![Javadocs](http://www.javadoc.io/badge/com.spotify.mobius/mobius-rx.svg?color=blue)](http://www.javadoc.io/doc/com.spotify.mobius/mobius-rx) / mobius-rx2 [![Javadocs](http://www.javadoc.io/badge/com.spotify.mobius/mobius-rx2.svg?color=blue)](http://www.javadoc.io/doc/com.spotify.mobius/mobius-rx2)
The rx modules contain extensions for RxJava. You should use one of them in your Mobius applications since they simplify creating effect handlers and event sources. Both RxJava modules share the same API, the only difference is that one is built for RxJava 1.x and the other for RxJava 2.x.

### mobius-android [![Javadocs](http://www.javadoc.io/badge/com.spotify.mobius/mobius-android.svg?color=blue)](http://www.javadoc.io/doc/com.spotify.mobius/mobius-android)
The android module primarily contains classes for hooking up a MobiusLoop to Android.
