The `Update` interface contains one method: `update(Model, Event)`. In this section we will refer to this method as the Update function. It is meant to be a function in the functional programming sense of the word, in other words it should be pure. In order for it to be a pure function, there are some constraints you should keep in mind:

## Keeping the Update function pure
- **The Update function must not have side-effects**. You are not allowed mutate any shared memory, write to disk, print a log message, make a network requests, etc. Whether your function has been called once or a million times should have no effect on the rest of the application. If there is any impact you want the function to have on the outside world, you should use an Effect to describe it.

- **The Update function must not depend on external state**. This is sort of the inverse of the previous statement. The only things that should have any effect on the return value of the Update function are the Model and the Event that are passed as arguments. This means no `System.currentTimeMillis()`, using `Random`, reading from disk, or even reading from shared memory. The only data you may use in the Update function is constants, the current Model object, and the current Event object.

- **Configuration of the Update function must be in the Model**. As a follow up to the previous statement, any configuration needed for the Update function must be put in the Model object. You should not have member fields that change the behaviour of the Update function, even if they are immutable.

## The return value of the Update function
There are four possible return values of the Update function: no change, only Effects, new Model, or both a new Model and Effects. These are described using the Next class, which has  correspondingly named factory methods:

```java
Next.noChange()
Next.next(model)
Next.next(model, effects)
Next.dispatch(effects)
```

If there are no Effects, then no Effect objects will be emitted by the Update, and likewise, if there is no new Model, then no Model will be emitted. Take special note of that last statement - if you don’t return a new Model then no Model will be emitted. It does not matter if the Model has actually changed or not, it is up to the Update function to decide if a Model should be emitted, and therefore whether observers (like the UI) will see a new Model or not.


## Tips for writing the Update function
- **Start by defining expected behaviours of the Update function**. We recommend a specific [modelling flow](https://github.com/spotify/mobius/wiki/The-Mobius-Workflow), and that you start by defining the Update function using unit tests. Doing so will help you uncover specification mistakes and corner cases before you spend any time on building out the Update function and, more importantly, the Effect Handlers and the UI.

- **Mobius is a FP pattern, but Java is not a FP language**. While the inspiration for Mobius comes from functional programming (FP), writing functional-style code isn’t always the best choice in Java. Sometimes using a for-loop and adding to an array just is easier than using a higher-order transform function and passing it an anonymous class instance. Try to keep your code simple and readable and avoid being too clever.

- **It is allowed to use mutable data structures inside the Update function**. The Update function only has to be pure from the outside. If you can simplify the implementation by setting up temporary mutable objects, go for it!

- **It is not necessarily a problem that the Update function is big**. As long as you have an easy to understand control flow, an Update function can become quite large without becoming hard to reason about. Do not hesitate to break it down into several smaller static Update functions, eg. break out the handling of a particular Event or everything that happens before data is loaded.

- **Avoid nesting too deeply**. Sometimes the Update function can become quite deep when you’re for example first switching on Event just happened, then which state you are in, and finally take action depending on data in the Model. In these cases you usually break out smaller functions that handle just this particular state, Event, etc. You can also use other regular techniques such as early returns or using local variables to store intermediate values.
