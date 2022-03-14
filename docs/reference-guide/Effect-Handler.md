Effect Handlers receive [Effects](Effect), execute them, and may produce [Events](Event) as a result. A [Mobius loop](Mobius-Loop) has a single Effect Handler, which usually is composed of individual [Effect Handlers](Effect-Handler) for each kind of Effect. (See [Mobius and RxJava](Mobius-and-RxJava) for how to compose RxJava-based Effect Handlers.) 

If the Effect handler needs data from the [Model](Model), that should always be passed along with the Effect. It would be possible for an Effect Handler to subscribe to Model updates, but that would introduce races and reduce simplicity.

Effect Handlers must never throw exceptions if something goes wrong. Doing so will crash or leave the loop in an undefined state. Instead, if an error occurs during Effect execution, that should be converted into an Event that can be used by the Update function to decide on how to proceed.

## Connections
An Effect Handler is connected to a `MobiusLoop` via a `Connection` - something the loop can use to send Effects to the handler, and to send an indication that it’s time to shut down. Once an Effect handler's Connection has been shut down (via the `dispose()` method), sending Events to the loop will cause an exception.

If an Effect Handler instance might be shared by multiple loops, extra care needs to be taken to ensure it’s safe for reuse. A way that this can happen in a Spotify context is if a user is viewing two different albums at the same time. Depending on how things are wired up, the same Effect handler might be reused for both albums.

If an Effect Handler can only be safely connected to a limited number of loops (usually that limit is 1), it should throw a `ConnectionLimitExceeded` exception if further connection attempts are made. This will prevent hard-to-find bugs in case Effect handlers unexpectedly end up being shared.
