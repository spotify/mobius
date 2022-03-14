# Mobius and Android

## Connecting a MobiusLoop to Android

As discussed when talking about configuration, a `MobiusLoop.Factory` is useful if you want to be
able to start the same loop many times from different starting points. One example of this is when
using a `MobiusLoop` in Android.

Whether you’re using Activities, Fragments, or some other abstraction, you typically have some
concept of restoring state. There may or may not be a saved state available when your component
starts, but if there is some saved state, you should start from it instead from starting from a
default state. On top of that, there is usually pause/resume where you have to pause execution and
resume from where you left off.

These cases are examples of starting from different model objects, and the reason why we
use `MobiusLoop.Factory` when connecting Mobius to Android. It allows Mobius to keep track of state
for you, and create new loops as required.

For our example we will start by creating a factory:

```java
MobiusLoop.Factory<MyModel, MyEvent, MyEffect> loopFactory =
    Mobius.loop(myUpdate, myEffectHandler)
       .init(myInit)
       .eventSource(myEventSource)
       .logger(AndroidLogger.tag("my_app"));
```

In this example we hook up the loop factory to a Fragment, but the same pattern applies for other
Android components with a lifecycle. We will create a `MobiusLoop.Controller` to help us control the
lifecycle of loops, which you normally do by calling `Mobius.controller(...)`.

However, in order to get model callbacks on the UI thread, we’ll use `MobiusAndroid.controller()`
instead. You can find it in the mobius-android module, and create it like this:

```java
MobiusLoop.Controller<MyModel, MyEvent> controller =
    MobiusAndroid.controller(loopFactory, MyModel.createDefault());
```

## Connecting the MobiusLoop.Controller to a Fragment

Now that we’ve created a `MobiusLoop.Controller`, we need to hook it up to the lifecycle events of
our Fragment:

```java
public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
  rootView = inflater.inflate(...);

  button = (Button) mRootView.findViewById(R.id.button);
  textView = (TextView) mRootView.findViewById(R.id.text);

  controller.connect(this::connectViews);

  if (savedInstanceState != null) {
    String value = savedInstanceState.getString("value");
    controller.replaceModel(MyModel.create(value));
  }

  return rootView;
}

@Override
public void onDestroyView() {
  super.onDestroyView();
  controller.disconnect();
}

@Override
public void onResume() {
  super.onResume();
  controller.start();
}

@Override
public void onPause() {
  super.onPause();
  controller.stop();
}

@Override
public void onSaveInstanceState(@NonNull Bundle outState) {
  super.onSaveInstanceState(outState);
  MyModel model = controller.getModel();
  outState.putString("value", model.getValue());
}
```

In this example we’re storing state using the regular state restore mechanism of Android, but you
could just as well use ViewModel (from Android Architecture Components) or any other mechanism to
keep track of model objects during configuration changes.

Most of this isn’t particularly strange or unexpected, but there is one part we’ve left
out: `this::connectViews`.

The argument to `MobiusLoop.Controller.connect(...)` is actually a `Connectable`, the same interface
that we used for effect handlers earlier. However this one is a `Connectable<M, E>` instead of
a `Connectable<F, E>` - in other words, it receives Models instead of Effects. We implement it in a
way similar to how we implemented the effect handler:

```java
private Connection<MyModel> connectViews(Consumer<MyEvent> eventConsumer) {
  // send events to the consumer when the button is pressed
  button.setOnClickListener(view ->
      eventConsumer.accept(MyEvent.buttonPressed()));

  return new Connection<MyModel>() {
    public void accept(MyModel model) {
      // this will be called whenever there is a new model
      textView.setText(model.getValue());
    }

    public void dispose() {
      // don't forget to remove listeners when the UI is disconnected
      button.setOnClickListener(null);
    }
  };
}
```

This becomes the one place where we hook up event listeners to the UI and update the UI based on the
model.

And that’s it: a new MobiusLoop gets created whenever the Fragment starts, and it’ll stop and
restart from where it left off whenever the fragment is paused/resumed. Furthermore, it supports
state restore, and it cleans up after itself when the Fragment is destroyed.

## Using RxJava with `MobiusLoop.Controller`

Just like RxJava helped us with effect handlers, it can also make our life easier when connecting a
UI. Both the effect handlers and the UI that you connect to `MobiusLoop.Controller` use the same
interface, so all utilities for `Connectables` can be used here, too. When it comes to RxJava, we
have `RxConnectables` that enable us to turn an `Observable` transformer into a `Connectable`.

Using [rxbinding](https://github.com/JakeWharton/RxBinding) makes implementing `connectViews` a very
nice experience. The connectViews we had before will now look like this:

```java
public Observable<MyEvent> connectViews(Observable<MyModel> models) {
  Disposable modelDisposable =
      models.subscribe(model -> textView.setText(model.getValue()));

  return RxView.clicks(button)
      .map(click -> MyEvent.buttonPressed())
      .doOnDispose(modelDisposable::dispose);
}
```

And the only other thing that we need to change is the `MobiusLoop.Controller.connect(...)` call:

```java
public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {

  // ...

  mController.connect(RxConnectables.fromTransformer(this::connectViews));

  // ...
}
```

And that’s it! If you have many event streams, you can combine them with `Observable.merge(...)`
/ `Observable.mergeArray(...)`, and if there are many subscriptions to the model, you can put them
all in a `CompositeDisposable`:

```java
public Observable<MyEvent> connectViews(Observable<MyModel> models) {
  CompositeDisposable disposables = new CompositeDisposable();

  disposables.add(models.subscribe(model ->
      textView1.setText(model.getValue1())));

  disposables.add(models.subscribe(model ->
      textView2.setText(model.getValue2())));

  return Observable.mergeArray(
          RxView.clicks(button1).map(c -> MyEvent.button1Pressed()),
          RxView.clicks(button2).map(c -> MyEvent.button2Pressed())
      ).doOnDispose(disposables::dispose);
}
```
