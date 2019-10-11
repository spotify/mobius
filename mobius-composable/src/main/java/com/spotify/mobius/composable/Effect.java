package com.spotify.mobius.composable;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Function;
import com.spotify.mobius.functions.Producer;

/**
 * An Effect is a representation of a side-effect that can be executed, and which produces a stream
 * of values of type T.
 */
public interface Effect<T> {

  interface Callback<T> {
    void onStart(Disposable disposable);

    void onValue(T value);

    void onComplete();
  }

  /**
   * Start running this effect, and use the given callback to report back values.
   *
   * @param callback
   */
  void execute(Callback<T> callback);

  static <T, U> Effect<U> map(Effect<T> effect, Function<T, U> f) {
    return (callback) ->
        effect.execute(
            new Callback<T>() {
              @Override
              public void onStart(Disposable disposable) {
                callback.onStart(disposable);
              }

              @Override
              public void onValue(T value) {
                callback.onValue(f.apply(value));
              }

              @Override
              public void onComplete() {
                callback.onComplete();
              }
            });
  }

  static <T> Effect<T> fromProducer(Producer<T> producer) {
    return callback -> {
      callback.onStart(() -> {});
      callback.onValue(producer.get());
      callback.onComplete();
    };
  }
}
