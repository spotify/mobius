# Mobius and RxJava

The primary use case for RxJava in Mobius is to execute effects, but because both are reactive
frameworks, they play quite well together in general. Since the update function in Mobius deals with
keeping track of state and deciding what should happen when, you can remove a lot of such logic from
your Observable chains. In a sense, Mobius can help you deal with state and state transitions in
RxJava.

For example, search-as-you-type is complicated to do with RxJava. Every time the text changes, you
send a request to the backend:

```java
Observable<String> input = // ...
Observable<Result> result = input.flatMap(text -> api.searchForText(text));
```

This would mostly work, but the problem is that **flatMap** doesn’t define in which order responses
come back, so you might get the wrong result arriving last!

You can use **concatMap**, but then you would instead block on waiting for all intermediate results,
so it’d get choppy if you type fast. You can use **switchMap**, but that aborts outstanding requests
when the text changes, so if you type faster than responses arrive, you won't see anything until you
stop typing.

Let’s say that the following is the behaviour we want:

> As long as we haven’t received the result that matches what’s currently entered into the text box,
> display all results as they come in, but when we get the response that matches the text box, 
> ignore all further results.

It is certainly possible to solve this type of problem with RxJava, but designing such observable
chains often end up requiring an in-depth understanding of RxJava, using very specific combinations
of operators, and understanding which values will be emitted where and when.

If you’re dealing with a problem that inherently is complex, Mobius won’t necessarily make it less
complex. However since Mobius has a single synchronization point through which all events go (that
is, the update function), you are usually able to express desired behaviours as rules by using
DataEnum’s `map(...)` function and simple `if`-statements. These rules typically are easier to test
and modify than a big RxJava chain, since it's just a pure function that describes them.

Let’s take a look at how the search-as-you-type behaviour can be implemented with Mobius. First we
need our data objects:

```java
@AutoValue
public abstract class Model {
  public abstract String query();
  public abstract Result result();
  public abstract boolean waitingForResult();

  // ... create, builder, etc.
}

@DataEnum
interface Event_datenum {
  dataenum_case TextChanged(String text);
  dataenum_case SearchResult(String query, Result result);
  dataenum_case SearchError(String query);
}

@DataEnum
interface Effect_datenum {
  dataenum_case SearchRequest(String query);
  dataenum_case ShowErrorMessage(String message);
}
```

Let’s write an update function for this:

```java
static Next<Model, Effect> update(Model model, Event event) {
  return event.map(
      textChanged -> {
          // always send a search request if the text changes, and
          // mark that we are waiting for results
          return next(model.toBuilder()
                  .query(textChanged.text())
                  .waitingForResult(true)
                  .build(),
              effects(Effect.searchRequest(text)));
      },

      searchResult -> {
        // ignore search results if we're not waiting for anything,
        // this allows us to drop events that arrive too late.
        if (!model.waitingForResult()) {
          return noChange();
        }

        // if the result query matches the model query, store the result
        // and stop waiting for any further results.
        if (model.query().equals(searchResult.query())) {
          return next(model.toBuilder()
              .result(searchResult.result())
              .waitingForResult(false)
              .build());
        }

        // if we are waiting for results, but this wasn't the result
        // we are waiting for, just update the model with this
        // intermediate result.
        return next(model.toBuilder()
            .result(searchResult.result())
            .build());
      },

      searchError -> { 
        // ignore search errors if we're not waiting for anything,
        // this allows us to drop errors if we already have a response to 
        // the query we're after.
        if (!model.waitingForResult()) {
          return noChange();
        }

        // if the query matches the model query, we need to tell the
        // user. 
        if (model.query().equals(searchError.query())) {
          return dispatch(effects(Effects.showErrorMessage("search request failed")));
        }
        
        // otherwise, just ignore the error, there are other requests in
        // flight that may succeed.
        return noChange();
      }
  );
}
```

This is not necessarily the best solution to search-as-you-type, but it reveals how we can express
our behaviours as rules in the update function. One benefit of the simplicity of the update function
is that it gives us a lot of flexibility. For example, you can easily modify it to use more complex
criteria for when an result should be ignored (for example, what if we’re erasing text in the field
and get a late result for a longer query?), or add a sequence number to each effect and ignore
anything that arrives out of order.

We can also implement request throttling, etc., in the update function, but it’s usually easier to
do that with RxJava.

Let’s create an effect handler for `Effect.SearchRequest` to see what this looks like. With RxJava
you implement effect-handling with `Observable` transformers, or by using references to methods that
have the same signature as transformers:

```java
public Observable<Event> effectHandler(Observable<Effect> effects) {
  return effects.ofType(Effect.SearchRequest.class)
      .debounce(200, TimeUnit.MILLISECONDS)
      .flatMap(request ->
          api.searchForText(request.query())
              .map(result -> Event.searchResult(request.query(), result))
              .onErrorReturn(err -> Event.searchError(request.query())));
}
```

We can use `RxConnectables.fromTransformer(...)` to convert the transformer into a Connectable and
pass it to `Mobius.loop(...)`, but it’s usually easier to just use `RxMobius.loop(...)` which does
that for you:

```java
MobiusLoop<Model, Event, Effect> loop =
        RxMobius.loop(Example::update, this::effectHandler)
            .startFrom(Model.create("", Result.EMPTY, false));
```


## RxMobius.subtypeEffectHandler()

One nice thing about the subclasses-approach of defining effects in Mobius is that we can use the
type of each effect to determine what it is. RxMobius has a utility that leverages this and enables
you to register an effect handler per effect type. We can write the handler for each individual
effect as a transformer, just like we did before, but instead of taking an Effect as argument it
takes specific effect types as its argument (for example, `Effect.SearchRequest`). The handler will
then only get effects of that type passed to it, and no casting is required:

```java
public Observable<Event> handleSearchRequest(
        Observable<Effect.SearchRequest> requests) {

  return requests
      .debounce(200, TimeUnit.MILLISECONDS)
      .flatMap(request ->
          api.searchForText(request.query())
              .map(result -> Event.searchResult(request.query(), result))
              .onErrorReturn(err -> Event.searchError(request.query())));
}
```

After writing individual handlers for all effects, we can then create a subtype effect handler and
register the handlers:

```java
ObservableTransformer<Effect, Event> rxEffectHandler =
    RxMobius.<Effect, Event>subtypeEffectHandler()
        // Effect Handlers can be an ObservableTransformer
        .addTransformer(Effect.SearchRequest.class, this::handleSearchRequest)
        // They can also be a Consumer<F> (eg. Consumer<ShowErrorMessage>)
        .addConsumer(Effect.ShowErrorMessage.class, view::showErrorMessage, AndroidSchedulers.mainThread())
        // Or an Action
        .addAction(Effect.SomethingElse.class, this::handleSomethingElse)
        // Or a Function<F, E> (eg. Function<SaveToDb, Event>)
        .addFunction(Effect.SaveToDb.class, this::handleSavingToDb)
        .build();
```

This means that eg. whenever a SearchRequest effect is received, it gets routed to
the `handleSearchRequest` observable transformer. All events from transformers are then merged back
into one stream and sent back to the update function.

The effect handler that `subtypeEffectHandler()` creates is an observable transformer too, so when
creating our loop we will again use `RxMobius.loop()` instead of `Mobius.loop()`:

```java
MobiusLoop<Model, Event, Effect> loop =
        RxMobius.loop(Example::update, rxEffectHandler)
            .startFrom(Model.create("", Result.EMPTY, false));
```
