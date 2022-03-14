Events are [immutable](Immutability) data objects that represent that something has happened. Events are what drive a [Mobius loop](Mobius-Loop) forward, and it is only when an Event is received that business logic is executed and state is evolved.

The purpose of Events is to allow the [Update](Update) function to know about when things happen and allow it to make decisions. This means that sending an Event is the only way to get new data into the Update function, and the only way to cause the [Model](Model) to change. In other words, all data you want to put into the Model when the loop is running has to be sent to Mobius using an Event, or be derived from data received in Events.

Events are also the only thing that can trigger an [Effect](Effect). This is because that just like with Model changes, it's the Update function that decides when Effects should happen, and those decisions are always triggered by Events.

Event names should describe things from the perspective of the business logic domain rather than from the UI design. For example, `LoginRequest` is a better event name in a search setting than `LoginButtonClicked`. A reason for that is that there may be more than one way of interacting with the UI that leads to triggering the same Event. For instance, you could request a login by hitting return from the password field, or by clicking on the login button.

> Note: Not everything occurring in a UI needs to be an Event. An example of that is something that happens purely in the UI without affecting the business logic, such as an animation. Intermediate steps in the animation should probably occur silently, but there might be an event emitted by the UI at the end of the animation, to trigger a transition to a different view. A good rule of thumb is: "Does the Update need to make any decisions when this happens?"

Events can be divided into three categories based on the source of the event: interaction, effect feedback, and external:

- **Interaction events**. This is the primary type of Events, and they are in a sense “the public API” of a Mobius loop. They are typically triggered by a user interacting with the UI (or something analogous for UI-less programs). Consequently, these Events are usually formulated as actions or intentions rather than desired behaviour. A couple of examples of this kind of Event are: `SearchQueryChanged`, `SkipTrackRequested`, `ShuffleClicked`.

- **Effect feedback events**. [Effect Handlers](Effect-Handler) will often need to communicate back progress of their effects. That feedback also takes the form of an Event. For example, loading data from a backend service could result in a `DataLoaded` event if the HTTP call succeeds or a `DataLoadingFailed` event if the call fails.

- **External events**. Some Events are neither interactions nor a result of an Effect - those are the external Events that you receive from an [Event Source](Event-Source). One way to think about the external Events are that they are feedback Events from the outside world. Examples of this kind of events are: ConnectivityChanged, LoginStateChanged, HeadphonesDisconnected.

Note that this distinction between different types of events is only used when reasoning about Events. In code, all Events are treated the same way, and there is no difference between the sources when an Event reaches the Update function.


## Guidelines for Events
- See the guide about [defining Events and Effects](Defining-Events-and-Effects).

- Events should have names based on user intent, and they will typically be in past tense. For example: `LoginRequested`, `QueryChanged`, `AddressChanged`, etc. 
