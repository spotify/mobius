> A Mobius loop receives [Events](Event), which are passed to an [Update](Update) function. As a result of running the Update function, the [Model](Model) might change, and [Effects](Effect) might get dispatched. The Model can be observed by the user interface, and the Effects are received and executed by an [Effect Handler](Effect-Handler).

![](https://github.com/spotify/mobius/wiki/mobius-diagram.png)

A Mobius loop is what ties everything together in the Mobius framework. It is responsible for sending events to the Update function, keeping track of the current Model, sending Effects to the Effect Handler, and listening to the [Event Source](Event-Source).

The normal way to create a loop is by calling `Mobius.loop(...)` and then starting it using `startFrom(...)`:

```java
MobiusLoop<Model, Event, Effect> loop =
    Mobius.loop(Example::update, this::effectHandler)
        .startFrom(Model.createDefault());
```

You can add an observer if you want to know when the Model changes, but the loop will start even if you don't add any observer:

```java
Disposable disposable = loop.observe(this::onModelChanged);
```

The returned disposable can be used to stop observing the loop, but normally you can just use `loop.dispose()` to shut down the loop and remove all observers at once.
