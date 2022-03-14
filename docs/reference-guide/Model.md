The Model is an [immutable](../patterns/Immutability.md) representation of the internal state and
configuration of a [Mobius loop](./Mobius-Loop.md). It contains two kinds of data, often
overlapping: what you need to make business logic decisions, and what you need to present in the UI.
The current Model and the incoming [Event](./Event.md) is the only input that
the [Update](./Update.md) function is allowed to consider, so anything needed to make decisions or
to generate output must be available there.

Because the Model is immutable, you will be required to create new instances of your Model whenever
it needs to change. This might at first glance seem wasteful, but thanks to everything being
immutable, only a shallow copy of the object is necessary – everything else can safely share
references with the previous version of the Model.

It might be tempting to put configuration or state as member fields of your Update implementation,
but if you do this then the Update function no longer is pure, which in turn means you can’t reason
about the loop in the same way. In principle you could break this rule, but just like breaking the
immutability contract, that breaks a lot of the assumptions in Mobius and means you should use
another framework.

When starting a Mobius loop, you will be required to provide a Model that the loop should start
from. It could simply be the initial state of the loop, but it could also be a previous Model you
want to resume execution from. Since the Update function of a Mobius loop doesn’t have any memory
other than the Model, any valid Model should be a valid a starting point for a new loop.

## Guidelines for Models

- **All the data you need in the Update function must be in the Model and Event**. This means any
  data used when making decisions must be represented in the Model. You should never keep state
  anywhere else within the Mobius loop.

- **The non-transient UI state should be derivable from the Model**. Some UI-only state is
  unavoidable, but it should be minimised as changes to UI are generally harder to understand and
  test for than changes in the Model. It will be easier to build a robust loop the simpler the UI
  is.

- **Avoid expressing UI concerns in the Model**. The Model should mostly be concerned with making
  decisions, not care about rendering. As the Model is used as an input for the rendering, there is
  of course a certain degree of coupling, but you should still try to express the Model in terms of
  what the meaning behind something is rather than the representation. An example of this is that in
  the Model, you should not say `isLoginButtonEnabled`, but rather `canLogin`. Strive to use this
  way of expressing yourself as much as possible, even if the Update function doesn’t make any
  decisions based on the value. But keep in mind that you should still use your own judgement to
  decide if it makes sense in your particular case. Sometimes it makes sense for the loop to be
  concerned about UI, in which case don’t go overboard trying to abstract away the idea.

- **Configuration must be part of the Model**. Configuration in this context could for example be
  the URI of the page (if this loop is for a user profile page, that would be the user profile URI),
  or which A/B-test group this user is in.

  If it is an A/B-test that changes behavior, you should not express it in terms of what the test
  is, but rather what behavior is changed. In other words you should not call
  it `isUserInMutualFriendsTestGroup`, but rather `shouldShowMutualFriends`. Translating from
  A/B-flags to configuration should be done when instantiating your Model.

- **Do not put behaviour in the Model**. The Model should be considered a value object. It is okay
  to put simple helper methods in the Model to make it easier to create new versions of it, but
  avoid making decisions in it. The Model in Mobius is just an object that holds some data, and
  shouldn’t be compared to the “model” of MVP or MVC, where it usually also contains domain logic.
