# Defining Events and Effects

Event and Effect objects are very similar. They have different roles in
a [Mobius loop](../reference-guide/Mobius-Loop.md), but both are [immutable](./Immutability.md) data
objects that get passed around as messages. As a consequence, both events and effects usually get
defined in the same way, and it is frequently a good idea to define them using the same pattern. In
the rest of this document we refer to them collectively as **messages**.

## Different ways to define messages

From the Mobius framework’s point of view, the message types are opaque, so it's up to you to define
what they are and what they mean. The sole constraint is all instances of a kind of message must
have a single type that they implement. So all Events for a given loop must share a type, and all
Effects must share a type.

There are many approaches to defining messages, but they can roughly be divided into the following
categories:

### Enumerations

This is the basic kind of message. Each message is an `enum`, an `int`, or even a `String`. There
can be no data attached to a dispatched message, so it's usually a limiting approach except in very
small or restricted loops. We occasionally use this type of message in the documentation for example
code.

### Tagged object

An extension to enumerations. It can for example be a simple java class with a couple of data fields
plus an extra "tag" that explains which kind of message it is, and therefore which fields are
supposed to be read.

Another example is to have a `Map<String, String>` with parameters, each specific message storing
things by using different keys in the map. However, this is not a type-safe approach and you must be
careful to only read "correct" parameters, and make sure that you enter the correct data when you
create the objects.

### Subclasses

This is the recommended way of defining messages. You have a common parent message type, for example
an interface `MyEvent`, and you make all your Events implement this interface. Each subclass can
then have its own data that makes sense for that particular message.

At first glance it looks like subclasses will make it tedious to check the type of each message and
manually cast them. In fact, it seems to possess some of the same problems as tagged objects. On top
of that, manually defining all the event and effect classes would lead to a lot of hard-to-maintain
and error-prone boilerplate code.

It might seem like a good solution to put an update method on each message subclass, and have the "
main" update function delegate to it. However, this is considered an anti-pattern in Mobius, as it
inserts business logic into messages that are supposed to be simple data carriers, thereby making
the code paths harder to follow, refactoring more difficult, and limiting your options in how you
structure the update function of the loop.

Even though there are some apparent hurdles, it turns out we have some tools at our disposal that
make working with subclasses a lot easier, as well as type-safe.

## Kotlin: Sealed classes

If you are using Kotlin, there's a perfect tool for
this: [sealed classes](https://kotlinlang.org/docs/reference/sealed-classes.html). Sealed classes
are a way to define subclasses that are "tied together" with a parent class, very much like a Java
enum:

```kotlin
sealed class MyEvent
data class Text(val text: String) : MyEvent()
data class Number(val number: Int) : MyEvent()
object Reset : MyEvent()
```

> Note: If no data is associated with a message, we define it as an `object` instead of a `data class`.

In your update function you are then able to use a when expression to "switch" the message type:

```kotlin
when (event) {
  // event is smart-cast to a "Text" and you can access event.text
  is Text -> /* code */

  // event is smart-cast to a "Number" and you can access event.number
  is Number -> /* code */

  // event is smart-cast to a "Reset" and there are no fields
  is Reset -> /* code */
}
```

This way you are only able to access fields that are available for each message, and defining the
messages gets very straight-forward.

## Java: DataEnum

Java has support for creating a kind of "sealed class" (using inner classes and a private super
constructor), but there is no mechanism for switching that is as easy to use as the when expression
in Kotlin.

To deal with this, we've created a companion library to Mobius
called [DataEnum](https://github.com/spotify/dataenum) (as in enum values with associated data). It
is an annotation processor that generates a kind of "sealed classes" similar to the ones you have in
Kotlin.

You define messages by creating an interface that looks like this:

```java
@DataEnum
interface MyEvent_dataenum {
  dataenum_case Text(String text);
  dataenum_case Number(int number);
  dataenum_case Reset();
}
```

It resembles the sealed classes in Kotlin, and you should think about it the same way. Based on this
specification, DataEnum generates a class `MyEvent` with inner subclasses `MyEvent.Text`
, `MyEvent.Number`, and `MyEvent.Reset`.

The `MyEvent` class also serves as a factory for creating events:

```java
MyEvent event1 = MyEvent.text("hello");
MyEvent event2 = MyEvent.number(42);
MyEvent event3 = MyEvent.reset();
```

Classes generated by DataEnum always have a `map` method, which works like Kotlin's when
expression (compare to the Kotlin code above):

```java
event.map(
   // event is cast to Text and you can access text.text()
   text -> /* code */,

   // event is cast to Number and you can access number.number()
   number -> /* code */,

   // event is cast to Reset and there are no fields
   reset -> /* code */
);
```

If you look closely you see that `map` is given three lambdas, and it works by making the event call
the lambda that matches its own type (basically it's a variation of the Visitor pattern). It's a bit
of a trick, but it leads to code that is type-safe and both looks and behaves like Kotlin's when
expressions.

You can read more about DataEnum and its capabilities
at [the project on GitHub](https://github.com/spotify/dataenum).

In most of the Mobius documentation we use and about DataEnum, but those patterns work equally well
with Kotlin sealed classes or manually defined messages.
