Let's build a simple "hello world" in Mobius. We'll create a simple counter that counts up or down
when we send events to the loop. We need to keep track of the current value of the counter, so we'll
be using an `Integer` as our model, and define an enum with events for increasing and decreasing the
value:

```java
enum Event { UP, DOWN }
```

When we get the up event, the counter should increase, and when we get the down event, it should
decrease. To make the example slightly more interesting, let's say that you shouldn't be able to
make the counter go negative. Let's write a simplified update function that describes this
behaviour ('simplified' in the sense of not supporting Effects - we'll get back to that later!):

```java
static int update(int counter, Event event) {
  switch (event) {
    case UP:
      return counter + 1;

    case DOWN:
      if (counter > 0) {
        return counter - 1;
      }
      return counter;
  }
}
```

We are now ready to create the simplified loop:

```java
MobiusLoop<Integer, Event, ?> loop =
        MobiusExtras.beginnerLoop(Example::update)
            .startFrom(2);
```

This creates a loop that starts the counter at 2. Before sending events to the loop, we need
to [add an observer](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/MobiusLoop.html#observe-com.spotify.mobius.functions.Consumer-)
, so that we can see how the counter changes:

```java
loop.observe(counter -> System.out.println(counter));
```

Observers always receive the most recent state when they are added, so this line of code causes the
current value of the counter to be printed: "2".

Now we are ready to send events! Let's put in a bunch of UPs and DOWNs and see what happens:

```java
loop.dispatchEvent(DOWN);    // prints "1"
loop.dispatchEvent(DOWN);    // prints "0"
loop.dispatchEvent(DOWN);    // prints "0"
loop.dispatchEvent(UP);      // prints "1"
loop.dispatchEvent(UP);      // prints "2"
loop.dispatchEvent(DOWN);    // prints "1"
```

Finally, you always want to clean up after yourself:

```java
loop.dispose();
```

## Adding Effects

One of Mobius’s strengths is its declarative style of describing side-effects, however in our first
example we had a simplified update function that didn't use any effects. Let’s expand it to show how
you dispatch and handle an effect.

Let's say that we want to keep disallowing negative numbers for the counter, but now if someone
tries to decrease the number to less than zero, the counter is supposed to print an error message as
a side-effect.

First we need to create a type for the effects. We only have one effect right now, but let's use an
enum anyway, like we did with the events:

```java
enum Effect { REPORT_ERROR_NEGATIVE }
```

The update function is the only thing in Mobius that triggers effects, so we need to change the
signature so that it can tell us that an effect is supposed to happen. In Mobius,
the [`Next<M, F>`](https://github.com/spotify/mobius/blob/master/mobius-core/src/main/java/com/spotify/mobius/Next.java)
class (many Mobius types are parameterised with one or more of `M`, `E`, and `F`, for Model, Event
and Effect respectively) is utilized to dispatch effects and apply changes to the model. Let's start
by changing the return type of the update function. The int we have used to keep track of the
current value of the counter is usually referred to as the model object in Mobius, so we change that
name too.

```java
static Next<Integer, Effect> update(int model, Event event) {
  switch (event) {
    case UP:
      return Next.next(model + 1);

    case DOWN:
      if (counter > 0) {
        return Next.next(model - 1);
      }
      return Next.next(model);
  }
}
```

Consider Next to be an object that describes "what should happen next". Therefore, the complete
update function describes: "given a certain model and an event, what should happen next?" This is
what we mean when we say that the code in the update function is declarative: the update function
only declares what is supposed to occur, but it doesn't make it occur.

Let's now change the less-than-zero case so that instead of returning the current model, it declares
that an error should be reported:

```java
static Next<Integer, Effect> update(int model, Event event) {
  switch (event) {
    case UP:
      return Next.next(model + 1);

    case DOWN:
      if (counter > 0) {
        return Next.next(model - 1);
      }
      return Next.next(model, Effects.effects(REPORT_ERROR_NEGATIVE));
  }
}
```

For the sake of readability you should statically import the methods on Next and Effects, so let's
go ahead and do that:

```java
static Next<Integer, Effect> update(int model, Event event) {
  switch (event) {
    case UP:
      return next(model + 1);

    case DOWN:
      if (counter > 0) {
        return next(model - 1);
      }
      return next(model, effects(REPORT_ERROR_NEGATIVE));
  }
}
```

That's it for the update function!

Since we now have an effect, we need an Effect handler. When an Update function dispatches Effects,
Mobius will automatically forward them to the Effect handler. It executes the Effects, making the
declared things happen. An Effect Handler can be thought of as a loop segment that connects the
Effect-dispatching part of the Update function with the Event-receiving part. An Effect Handler is a
function from
a [`Consumer<Event>`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/functions/Consumer.html)
- the place where it should put generated Events - to
a [`Connection<Effect>`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/Connection.html)
- the place where Mobius should put Effects, and where it can request shutdown.

The basic shape looks like this:

```java
static Connection<Effect> effectHandler(Consumer<Event> eventConsumer) {
  return new Connection<Effect>() {
    @Override
    public void accept(Effect effect) {
      // ...
    }

    @Override
    public void dispose() {
      // ...
    }
  };
}
```

If you're used to [`Observables`](https://github.com/ReactiveX/RxJava/wiki/Observable), this may
look backwards. It's because Mobius
uses [`Consumers`](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/functions/Consumer.html)
that you push things to rather than Observables that you receive things from.

The effect handler gets connected to the loop by the framework when the loop starts. When
connecting, the handler must create a
new [`Connection`](https://github.com/spotify/mobius/blob/master/mobius-core/src/main/java/com/spotify/mobius/Connection.java)
that Mobius uses to send Effect objects to the Effect handler. The Event consumer is used for
sending events back to the update function, however it is important that the handler respects
the `dispose()` call. This means that when `dispose()` is called, no more events may be sent to the
event consumer. Furthermore, any resources associated with the connection should be released when
the connection gets disposed.

In this case we have a very simple effect handler that doesn’t emit any events and therefore ignores
the `eventConsumer`:

```java
static Connection<Effect> effectHandler(Consumer<Event> eventConsumer) {
  return new Connection<Effect>() {
    @Override
    public void accept(Effect effect) {
      if (effect == REPORT_ERROR_NEGATIVE) {
        System.out.println("error!");
      }
    }

    @Override
    public void dispose() {
      // We don't have any resources to release, so we can leave this empty.
    }
  };
}
```

Now, armed with our new update function and effect handler, we're ready to set up the loop again:

```java
MobiusLoop<Integer, Event, Effect> loop =
        Mobius.loop(Example::update, Example::effectHandler)
            .startFrom(2);

loop.observe(counter -> System.out.println(counter));
```

Like last time it sets up the loop to start from "2", but this time with our new update function and
an effect handler. Let's enter the same `UP`s and `DOWN`s as last time and see what happens:

```java
loop.dispatchEvent(DOWN);    // prints "1"
loop.dispatchEvent(DOWN);    // prints "0"
loop.dispatchEvent(DOWN);    // prints "0", followed by "error!"
loop.dispatchEvent(UP);      // prints "1"
loop.dispatchEvent(UP);      // prints "2"
loop.dispatchEvent(DOWN);    // prints "1"
```

It prints the new error message, and we see that it still prints a zero. However, we would like to
get only the error message, and not the current value of the counter.
Fortunately [Next](https://javadoc.io/page/com.spotify.mobius/mobius-core/latest/com/spotify/mobius/Next.html)
has the following four static factory methods:

|                | Model changed             | Model unchanged        |
|----------------|---------------------------|------------------------|
| **Effects**    | Next.next(model, effects) | Next.dispatch(effects) |
| **No Effects** | Next.next(model)          | Next.noChange()        |

This enables us to say either that nothing should happen (no new model, no effects) or that we only
want to dispatch some effects (no new model, but some effects). To do this you use `Next.noChange()`
or `Next.dispatch(effects(...))` respectively. We don't make any changes to the model in the
less-than-zero case, so let's change the update function to use `dispatch(effects(...))`:

```java
static Next<Integer, Effect> update(int model, Event event) {
  switch (event) {
    case UP:
      return next(model + 1);

    case DOWN:
      if (counter > 0) {
        return next(model - 1);
      }
      return dispatch(effects(REPORT_ERROR_NEGATIVE));
  }
}
```

Now let's send our events again:

```java
loop.dispatchEvent(DOWN);    // prints "1"
loop.dispatchEvent(DOWN);    // prints "0"
loop.dispatchEvent(DOWN);    // prints "error!"
loop.dispatchEvent(UP);      // prints "1"
loop.dispatchEvent(UP);      // prints "2"
loop.dispatchEvent(DOWN);    // prints "1"
```

Success!

In this case we merely printed the error to the screen, but you can imagine the effect handler doing
something more sophisticated, maybe flashing a light, playing a sound effect, or reporting the error
to a server.

## Using DataEnum and AutoValue

When using Mobius you usually want to use a bit more expressive type for out Model, Event, and
Effect classes than just ints and enums. Our example with a counter is quite simple, so using more
expressive types wouldn’t really help making things easier to understand.

Because the counter is a simple example, and
because [DataEnum](https://github.com/spotify/dataenum) (Algebraic data types for Java)
and [AutoValue](https://github.com/google/auto/tree/master/value) (Immutable value types for Java)
are recommended when defining Model, Events, and Effects, we’ll modify the counter to show the
basics of DataEnum and AutoValue usage.

We start by defining our new Model, Event, and Effect types:

```java
@AutoValue
public abstract class Model {
  public abstract int counter();

  public Model increase() {
    return create(counter() + 1);
  }

  public Model decrease() {
    return create(counter() - 1);
  }

  public Model create(int counter) {
    return new AutoValue_Model(counter);
  }
}

@DataEnum
interface Event_datenum {
  dataenum_case Up();
  dataenum_case Down();
}

@DataEnum
interface Effect_datenum {
  dataenum_case ReportErrorNegative();
}
```
Now let’s change the update function and effect handler to use the new types:
```java
static Next<Model, Effect> update(Model model, Event event) {
  return event.map(
    up -> {
      return next(model.increase());
    },

    down -> {
      if (model.counter() > 0) {
        return next(model.decrease());
      }
      return dispatch(effects(Effect.reportErrorNegative()));
    }
  );
}

static Connection<Effect> effectHandler(Consumer<Event> eventConsumer) {
  return new Connection<Effect>() {
    @Override
    public void accept(Effect effect) {
      // effect.match() is like event.map() but has no return value
      effect.match(
        reportErrorNegative -> System.out.println("error!")
      );
    }

    @Override
    public void dispose() {
      // No resources to release.
    }
  };
}
```

In this effect handler, we just print an error message to standard out, which is about the simplest
possible side-effect you can have. On top of that, we’re only handling a single effect.

A typical loop might contain many effects that need to be handled, and they are often asynchronous.
On top of that, no further events may be emitted after `dispose()` is called, so if you have a lot
of things going on here then it can get quite messy to deal with cleaning everything up.

Luckily, asynchronicity and cleaning up is precisely what RxJava is good at!

If you squint a bit, you might be able to tell that the effect handler resembles Observable
transformers from RxJava. They are in fact compatible: the mobius-rx / mobius-rx2 modules contain
utilities to convert to and from Observable transformers, so that you can use transformers as effect
handlers. You’ll get some examples of this in the [next section](../getting-started/Mobius-and-RxJava.md).
