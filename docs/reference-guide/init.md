# Init

The `init(Model)` function is called first when a [Mobius loop](./mobius-loop.md) is started, and
can be seen as a special version of the [Update](./update.md) function. It takes
a [Model](./model.md) just like Update, but it does not have any Event as an argument. It returns
a `First` instead of a `Next`, the difference being that a `First` always contains a Model, while
a `Next` sometimes contain nothing or only [Effects](./effect.md).

One good way to think of the Init function is as a request to “resume execution from a particular
Model”. This means put the loop in a valid state and send off any required Effects to activate it.

An example of putting the loop in a valid state might be that if the old Model was showing an
“offline” state, when you restore maybe it would be better to start from a “loading” state and send
an Effect to load data. Likewise if the Model already was in a “loading” state, the Init function is
responsible for sending an Effect to restart the loading of data. If we don’t do that, then the data
won’t ever load, and the user would be stuck in the loading state forever!

Providing a custom Init function is not required, but you need to make sure that this doesn’t lead
to the loop ending up in undesired states that the user can’t get out of.

## Guidelines for the Init function

The same guidelines apply as [for the Update function](./update.md).

