# Configuring a MobiusLoop

So far, we’ve started the loop in the following manner:

```java
MobiusLoop<Model, Event, Effect> loop =
        Mobius.loop(Example::update, effectHandler)
            .startFrom(2);
```

This means providing only the mandatory parameters (an Update function and an Effect handler) of
a [`MobiusLoop`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/MobiusLoop.html), 
and using default values for the rest. This section describes what the optional parameters are and
how to configure them.

Previously, we've immediately started the loop by the call to `startFrom(...)`. If we don't call it,
we instead get
a [`MobiusLoop.Builder<M, E, F>`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/MobiusLoop.Builder.html):

```java
MobiusLoop.Builder<Integer, Event, Effect> loopBuilder =
        Mobius.loop(Example::update, Example::effectHandler);
```

(`RxMobius.loop()` also returns a `MobiusLoop.Builder<M, E, F>`)

The builder enables us to configure the loop before we start it. It's an immutable object, so it's
therefore safe to share and reuse it to start several loops from the same configuration. If you pass
it around as a dependency, consider upcasting it
to [`MobiusLoop.Factory`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/MobiusLoop.Factory.html)
, which is the interface that contains only `startFrom(...)`. This enables dependents to start new
loops but not create new configurations based on the old one.

Here's what you can configure in `MobiusLoop.Builder` - details of each item are described below:

```java
MobiusLoop<Model, Event, Effect> loop =
        Mobius.loop(Example::update, effectHandler)
            .init(Example::init)
            .eventSource(Example::externalEvents)
            .eventRunner(WorkRunners.singleThread())
            .effectRunner(WorkRunners.fixedThreadPool(2))
            .logger(SLF4JLogger.withTag("Example Loop"))
            .startFrom(Model.createDefault());
```

### `init(...)`
NOTE: deprecated; prefer using either the MobiusController, or `startFrom(model, effects)` instead.

An init function resembles an update function, but it is only executed once when the loop starts (
read in the [concepts guide about init](../concepts.md#starting-and-resuming-a-loop) for details on
why you might want to have an init function). The differences to update is that the init function
doesn't get any event (you could say there is an implicit "the loop is starting" event) and that it
returns a First instead of a Next. A First must contain a model, but apart from that the two classes
are the
same.

### `eventSource(...)`

The `EventSource` of your program is supposed to be used for external events 
(see [Events in depth](../reference-guide/event.md) for more details). If you have multiple external
event sources, they must be merged into a single EventSource before being hooked with with Mobius.

Consider an event source as an effect handler without incoming effect objects and that it just emits
events on its own. Basically an event source lets the loop listen to external signals, for example
network connectivity changes or timer ticks. UI events should not be sent to the loop via the event
source, but instead from the outside of the loop through `MobiusLoop.dispatchEvent()`. It is
possible to send UI events through an event source, and once an event reaches the update function
there is no difference between the origins of the events. However avoid sending UI events through
the event source, as it is intended for external events only.

You must be careful if you implement the `EventSource` interface directly. It has the same
requirements as effect handlers, that is, it must release resources and stop emitting events when it
is disposed (see the javadocs on `EventSource` for details). In most cases you should instead
use `RxEventSources` which takes care of all that for you.

### `logger(...)`

The `MobiusLoop.Logger` interface enables you to inspect what the init and update functions are
doing. Every event, effect, and model change in the loop will go through the logger, so if there are
any issues with your loop, the logs tells you what state the loop was in when the problem happened,
and how it got there. Because of the usefulness of loggers, we recommended that you always set one
in debug builds. Mobius provides two implementations of Logger: `SLF4JLogger` in mobius-extras
and `AndroidLogger` in mobius-android.

### `eventRunner(...)` / `effectRunner(...)`

Mobius
uses [`WorkRunner`s](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/runners/WorkRunner.html)
to execute work on different threads. A `MobiusLoop` has two of these internally: one for events (
for example, the thread that runs the update function) and one for effects (for example, the thread
that sends effects to the effect handler). Since the builder can be used to start multiple loops,
you will have to pass `WorkRunner` factories to the builder. Usually you don't need to override
this, but it can be useful in integration tests to use `WorkRunners.immediate()` in order to
make `MobiusLoop` synchronous. Other than the work runners in
the [`WorkRunners`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/runners/WorkRunners.html)
class, there is also a `SchedulerWorkRunner` in mobius-rx/mobius-rx2.