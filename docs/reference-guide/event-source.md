# Event Source

An Event Source is used to send external signals into a [Mobius Loop](./mobius-loop.md)
as [Events](./event.md). The typical use case is to listen for things like:

- Network connectivity changes (online/offline)
- Timers (periodic ticks, eg. once per second)
- Headphones connected/disconnected
- Events received by `BroadcastReceiver`s
- etc.

In principle an Event Source could be used to send events from the UI (like clicks) but this is
discouraged. Instead prefer using `MobiusLoop.dispatchEvent(...)` or a `Connectable` if you
use `MobiusLoop.Controller`.

Structurally an Event Source is very similar to [Effect Handlers](./effect-handler.md) but an Event
Source does not need any Effects to be triggered before starting to send Events.

You can configure it by calling `.eventSource(...)` on a `MobiusLoop.Builder`:

```java
MobiusLoop.Builder<Model, Event, Effect> loopBuilder = Mobius.loop(update, effectHandler)
    .eventSource(myEventSource);
```

If you are using RxJava you can wrap any Observables that emits your Event type into an `EventSource`:

```java
Observable<Event.First> first = ...
Observable<Event.Second> second = ...
Observable<Event.Third> third = ...

EventSource<Event> eventSource = RxEventSources.fromObservables(first, second, third);
```