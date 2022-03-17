# Effect

Effect objects are used by the [Update](./update.md) function to request that the
[Effect Handler](./effect-handler.md) executes impure code. Impure code either has measurable
side-effects or has an output that isn't completely determined by its input parameters. For example,
this can be sending a request to a backend service, reading something from disk, changing the value
of a shared (global/singleton) object, etc.

The objects themselves are [immutable](../patterns/immutability.md) data objects just like the Model
and Event objects. Effects and Events are similar in that they are both messages. The difference is
in the direction as seen from the Update function - an Event is something that happened that the
business logic needs to react to, whereas an Effect is something that the business logic wants to
make happen in the outside world.

Effects are commands, in the CQRS definition of the term. Note that Mobius provides no guarantees
regarding the execution order of Effects - not even in the sense that Effects resulting from Event N
will be processed before Event N+1 is.

# Guidelines for designing Effects

- Use imperative form in the names, reflecting what should happen: `SendLoginRequest`, `PersistUser`
  , `LoadPlaylistData`, etc.

- Effects should be value objects without business logic.

- Prefer using something similar to DataEnum (in Java) or sealed classes (in Kotlin) for Effect
  definitions.