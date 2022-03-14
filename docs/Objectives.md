# Objectives

Mobius was designed with some specific objectives in mind, and it’s going to be easier both to use
it correctly and to understand some of the design decisions if you know the underlying thinking. The
original objectives we set out to achieve were:

- Strong **separation of concerns** in general, and specifically between presentation logic and
  business logic.

- Encourage maximal use of **pure functions**.

- Great **testability** through the design itself as well as included tooling.

- Ease of understanding systems' behaviour in terms of **concurrency and ordering**.

The next couple of sections describe how Mobius achieves those objectives.

## Separation of Concerns

Mobius ensures or encourages separation in the following ways:

- **Separating what should happen from how it happens**. Effects are requested by the Update
  function, and performed by Effect handlers.

- **Separating handling of different kinds of Effect from one another**. Through making it easy and
  recommended to implement individual Effect handlers for each Effect.

- **Clarifying that UI state is different from loop state (the current Model)**. Most of the UI
  state will be derivable from the loop state/Model, but not all of it. Examples of the latter
  include things like the current position in a text field, the current state of animation (usually)
  , etc. It’s perfectly OK to have state in the UI, but you maximise the benefit you get from Mobius
  by minimising that state.

- **Processing of Events happens sequentially, one Event at the time**. This means that there can be
  no data races in the Update function. However, note that Mobius doesn't provide any guarantees at
  all about Event or Effect ordering, and we strongly recommend that you ensure that your Update
  functions are robust in the face of out-of-expected-order Events (strictly speaking, since there
  are no ordering guarantees, there can be no out-of-order Events).

## Pure Functions

Mutable state makes programs hard to understand, because it introduces the notion of change over
time. To understand how something behaves, you must understand what has happened before so you can
know the current state of the object. Pure functions, on the other hand, are predictable, making
them easier to understand and test because their behaviour is entirely defined by their inputs. But
mutable state is necessary in any interesting program. Mobius restricts mutable state to the
following places, allowing you to implement the rest of the code as pure functions:

- **In the UI**: Most UI frameworks have lots of mutable state, such as the contents of or cursor
  position in a text field, the current state of an animation, etc. This state is separate from the
  Model, which holds the business logic state.

- **Internals of MobiusLoop**. The Model object must be immutable, but to support changing the
  internal state of the loop, Mobius tracks a reference to the current Model. As a user, you don’t
  need to worry about this, because your Update and Init functions are pure functions.

- **Persistent or feature-external state**, such as the ‘recent searches’ history tracked by a
  Search feature, or the state of a Spotify user’s playlists. A change to this type of state is made
  using an Effect that describes the desired change, and an Effect Handler that executes the actual
  change.

## Testability

We believe having a great set of tests for your code is one of the best ways to ensure you can be
productive when coding, because great tests quickly give you feedback about mistakes made when you
evolve your code. Great tests are characterised by:

- Short run times to get quick feedback cycles - subsecond is ideal.

- Small scope for errors => more precise reporting of what's broken. When something breaks, it
  should be obvious where in the system it broke.

- Sufficient coverage of branches and lines.

- Loose coupling with production code. Tests that use a lot of mocks and other ways of verifying
  that the system under test had side effects tend to be closely tied to the production code. It is
  better to write tests as "Given an initial state, When X happens, Then the state is Y" - rather
  than, "Then method A on collaborator B was invoked with parameters C and D".

- Adhering to the Single Responsibility principle - a test should have only one reason to change
  and/or fail. Violating the SRP for tests is a thing that greatly reduces evolvability of the
  system, because intended changes to production code lead to lots of unrelated changes to test
  code. The test code becomes a dead weight you have to drag around.

Mobius helps to achieve these kinds of tests through:

- Pure functions that make it easy to get great coverage. They have no state-space, so less
  combinatorial explosion internally.

- Pure functions that make it unnecessary to verify side effects through mocks and spies.

- Strong separation of concerns that enables writing small tests (in the sense of how much
  production code a test covers). Small tests are faster and more precise, and more likely to have a
  single responsibility.

- Testing utilities (
  see [UpdateSpec](https://github.com/spotify/mobius/blob/master/mobius-test/src/main/java/com/spotify/mobius/test/UpdateSpec.java)
  and [InitSpec](https://github.com/spotify/mobius/blob/master/mobius-test/src/main/java/com/spotify/mobius/test/InitSpec.java)
  especially) that make it convenient to write good outside-in tests.

- A [recommended workflow](./The-Mobius-Workflow.md) that includes writing business logic tests before
  implementing it. This helps ensure there is no coupling with the production code - since it hasn’t
  been written yet, it’s not possible to couple your tests to it.

## Concurrency and Ordering

It is notoriously hard to write robust code when things can happen in any order, and in mobile apps,
changes can come from the user, the system, or a backend service with no way of effectively
controlling ordering.

The first property of Mobius that helps make understanding a loop's behaviour is the sequential and
atomic Event processing. Since the only thing that can change the loop’s state is an Event arriving,
and only one Event is processed at a given time, there are no data races in the loop itself. In
contrast, a multi-threaded solution where a click listener might be modifying some mutable state
concurrently with a response from backend server gets harder to understand.

Second, since all Events and Effects are simple value objects, the history of processed Events and
resulting Models and Effects provides excellent troubleshooting information.

Mobius provides no guarantees about the execution order of Effects or arrival order of Events. The
reason is that that’s what the world is like, when you’re doing distributed computing. You never
know what’s going to happen to requests you send somewhere else. So if you send a request from a
client to some backend server, and you don’t get a response, that could be caused by any of the
following:

- The request got lost somewhere
- The response got lost somewhere
- The request or response is still en route and a response will arrive if you just wait ‘a little
  longer’

An implication of that is that if you send two requests from a client to a backend service, you
don’t know that the responses will arrive in the same order as the requests were sent. So in Mobius,
we recommend that you ensure that your business logic can handle Events arriving in any order in a
robust way. An example that shows some of the complexity is a signup process where the following
happens:

1. **Event** `SignupRequested`: The user clicks "register".

1. **Effect** `CreateAccount(username, password)`: The Update function tries to create an account
   using an effect.

1. **Event** `AccountCreationTimedOut()`: The Effect Handler decides a response is unlikely to
   happen.

1. **Event** `SignupRequested`: The user clicks "retry".

1. **Effect** `CreateAccount(username, password)`: The Update function tries to create an account
   using an effect again.

1. **Event** `AccountCreationSucceeded`: Delayed response from the first attempt finally arrives.

1. **Event** `DuplicateUsernameError`: Response from the second attempt.

This gets tricky! Especially if the `CreateAccount` Effect Handler is implemented in such a way it
no longer listens to the response from the first attempt after the timeout. In that scenario, the
user’s account has in fact been created, but there is no way to allow them to proceed to the next
step since the client’s business logic doesn’t know this.

This is in fact even more complicated, because the `AccountCreationSucceeded` response might get
lost entirely – a real solution entails making the `CreateAccount` messages idempotent and some way
for the backend to detect that two `CreateAccount` messages are in fact duplicates and not submitted
by different users more or less concurrently.

Mobius provides test tools (
see [`UpdateSpec.When`](https://javadoc.io/page/com.spotify.mobius/mobius-test/latest/com/spotify/mobius/test/UpdateSpec.When.html))
that help you write test cases that validate the behaviour of your loop when a sequence of Events
happen given an initial Model. This, combined with the sequential Event processing, helps you write
robust code in a concurrent setting where Event ordering cannot be guaranteed.
