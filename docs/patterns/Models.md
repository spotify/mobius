Just as Events and Effects, Model objects are opaque to the Mobius framework itself. They should be
immutable, but other than that they can be anything.

Since the update function in Mobius represents state transitions in a state machine, it’s natural to
see the model as representing the current state of that machine. When defining a model for the state
machine, a spectrum of approaches is available to us, ranging from a strict finite-state-machine
approach, to a more loosely defined “put everything in a bucket” approach.

### All states use different classes

When you consider finite-state machines, having one class per state makes sense. The machine can
only be in one state at the time, and each state only possesses data that makes sense in that state.
Let’s draft a small example of this using DataEnum:

```java
@DataEnum
interface Model_dataenum {
  dataenum_case WaitingForData();
  dataenum_case Loaded(String data);
  dataenum_case Error(String message);
}
```

We now have three classes, `WaitingForData`, `Loaded`, and `Error`, and at a given time our model
can only be one of them.

As you see, the `data` field only exists in the `Loaded` state, so you don’t have to check
for `null` when accessing it, because you will only be `Loaded` if `data` is non-null. This approach
is perfect for small loops with few states, or when you want to be assured that all corner cases are
covered.

However, there are some drawbacks to this approach, particularly when there are many states that
start overlapping. For example, if there is an “offline” state, you might want to distinguish
offline-but-no-data from offline-but-with-data ‒ this quickly leads to an explosion of the number of
states and state transitions that must covered, and you might end up with plenty of boilerplate just
to copy data from one state to another.

### All states use the same class

This approach is on the other end of the spectrum compared to the previous one. You use flags to
keep track of whether data is loaded, etc., and store everything at the object’s “top level”. Let’s
look at AutoValue for this example, and let’s include offline as an extra flag, too:

```java
@AutoValue
public abstract class Model {
  public abstract boolean loaded();
  public abstract boolean error();
  public abstract boolean offline();
  @Nullable
  public abstract String data();
  @Nullable
  public abstract String errorMessage();

  // ... create method and/or builder, etc. ...
}
```

> Note: You might end up with a lot fields that can be `null`. There can also be invalid state 
> combinations (in the case above, both loaded and error can be true at the same time), or cases 
> with both data and an error message. This is of course an exaggerated case, but when you approach 
> this end of the spectrum, you might get more special cases that must be handled carefully.

This kind of model tends to be easier to modify than the previous approach when requirements change
and new states are required, and it is a lot easier to create new versions of model objects from old
ones, especially if you use AutoValue's `toBuilder()`.

It is often advantageous to start with this kind of model, as it is the most straightforward one to
create and the easiest one to evolve as requirements change.

### Hybrid approach

One good way to gain the conveniences of a single model, but still avoid invalid states, is to
borrow some ideas from both previous approaches and go for a hybrid solution.

The first model provided a good way to deal with the regular states, and it was its offline scenario
that messed things up. So instead of duplicate all states of the first model, let’s combine the
first approach with the second one:

```java
@DataEnum
interface LoadingState_dataenum {
  dataenum_case WaitingForData();
  dataenum_case Loaded(String data);
  dataenum_case Error(String message);
}

@AutoValue
public abstract class Model {
  public abstract boolean offline();
  public abstract LoadingState loadingState();

  // ... create method and/or builder, etc. ...
}
```

Now it’s possible to be both loaded and offline at the same time! We’ve combined two state machines
by putting them next to each other ‒ one keeps track of data loading, and the other keeps track of
whether you’re offline. Also, this approach scales up to multiple parallel state machines, or even
state-machines-within-state-machines.

Note that this isn’t necessarily a perfect model: for example, maybe the waiting-for-data and
offline states are incompatible. If it’s really important for you to deal with this state in the
model, you’d have to go for something a bit more like the first approach, but if it’s just a single
combination that is troublesome now, the hybrid solution is often a worthwhile trade-off.

The hybrid provides a more flexible model that is easier to modify when requirements change, and
you’re still avoiding most edge cases (for example, in this version data is never null, and you
can’t have both data and an error message).

## Some useful tricks for model objects

Since model objects are supposed to be immutable, you need to create new ones whenever you want to
change anything. Since this will be a common occurrence, and you want code to be easy to read, you
should create helper methods to carry out these changes. In this section we will look at some ways
you can do this.

Let’s imagine we have a Model for a todo-list. It might look something like this:

```java
@AutoValue
public abstract class Task {
  public abstract String description();
  public abstract boolean complete();

  public static Task create(String description, boolean complete) {
    return new AutoValue_Task(description, complete);
  }
}

@AutoValue
public abstract class Model {
  public enum Filter {
    ALL,
    INCOMPLETE,
    COMPLETE
  }

  public abstract List<Task> tasks();
  public abstract Filter filter();

  public static Model create(String description, Filter filter) {
    return new AutoValue_Model(tasks, filter);
  }
}
```

> Note: Avoid arrays and Lists in the model like this, since they are mutable. Instead you should 
> use something like `ImmutableList` from Guava. That being said, we use `List`s in these examples 
> to keep them short.

### Java: AutoValue

If you use AutoValue (recommended when you use Java), `builder()` and `toBuilder()` will be your
best friends. Define a builder like this:

```java
@AutoValue
public abstract class Task {

  // ...

  public static Builder builder() {
    return new AutoValue_Task.Builder();
  }

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder description(String description);

    public abstract Builder complete(boolean complete);

    public abstract Task build();
  }
}
```

> Note: Writing this by hand can get cumbersome. You can make your job easier by using one of the 
> AutoValue plugins for IntelliJ to generate it automatically.

Now we either create a new `Task` from scratch with a fluent API:

```java
Task task1 = Task.builder()
    .description("hello")
    .complete(false)
    .build();
```

Or we create modified versions of an existing object:

```java
Task task2 = oldTask.toBuilder()
    .complete(true)
    .build();
```

You can also set default values by applying them in the static `builder()` method:

```java
public static Builder builder() {
  return new AutoValue_Task.Builder()
      .complete(false);
}
```

### Kotlin: Data classes
If you use Kotlin, things get a bit easier: use data classes and their `.copy()` method:

```kotlin
data class Task(val description: String, val complete: Boolean)

val task1 = Task("hello", false)
val task2 = task1.copy(complete=true)
```

### `with`-methods
The builders are powerful, but sometimes they don’t read well in the Update function:

```java
static Next<Model, Effect> update(Model model, Event event) {
  // ...

  return event.map(
      // ...
      completeChanged -> {
        int index = completeChanged.index();

        Task oldTask = model.tasks().get(index);
        Task newTask = oldTask.toBuilder()
            .complete(completeChanged.complete())
            .build();

        List<Task> newTasks = new ArrayList<>();
        newTasks.addAll(model.tasks());
        newTasks.set(index, newTask);

        return next(model.toBuilder()
            .tasks(newTasks)
            .build());
      }
      // ...
  );
}
```

A lot of the noise is due to us working with immutable objects, but it doesn’t have to be this
messy. There is a lot going on in this function and it’s not easy to see what it is:

- The old task is fetched from the old list of tasks
- A new task is created from the old task
- A new list is created from the old list
- The old task is replaced by the new task in the new list
- The old list is replaced by the new list in the model

This is merely mechanical juggling of data - what we’re really trying to say is:

- Change `complete` of the task at position `index`.

In other words, we’d like our update function to look like this:

```java
static Next<Model, Effect> update(Model model, Event event) {
  // ...

  return event.map(
      // ...
      completeChanged -> {
        return next(model.withTaskComplete(
            completeChanged.index(),
            completeChanged.complete()));
      }
      // ...
  );
}
```

We implement this step-by-step in the model, starting with the `Task` class:

```java
@AutoValue
public abstract class Task {
  // ...

  public Task withComplete(boolean complete) {
    return toBuilder()
        .complete(complete)
        .build();
  }
}
```

This enables us to easily create copies of a `Task` with a different value for `complete()`. We can
also use the same pattern to replace a single task in `Model`:

```java
@AutoValue
public abstract class Model {
  // ...

  public Model replaceTask(int index, Task task) {
    List<Task> newTasks = new ArrayList<>();
    newTasks.addAll(tasks());
    newTasks.set(index, task);

    return toBuilder()
        .tasks(newTasks)
        .build();
  }
}
```

Finally we combine the methods and create our `withTaskComplete(...)`:

```java
@AutoValue
public abstract class Model {
  // ...

  public Model withTaskComplete(int index, boolean complete) {
    Task oldTask = tasks().get(index); 
    return replaceTask(index, oldTask.withComplete(complete));
  }
}
```

We still have to do more or less the same things, but this a more fluent API for the job. We are
also able to reuse some of these helper methods. For example, `Model.replaceTask(...)` will be
useful if we want to change the description of a task.