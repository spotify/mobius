/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius.rx;

import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.functions.Func1;

/**
 * Utility that dispatches each item emitted from a source observable of type T to multiple other
 * Observable.Transformers, and merges the results to a stream of type R.
 *
 * @param <T> input type
 * @param <R> output type
 */
class MergedTransformer<T, R> implements Observable.Transformer<T, R> {

  private final Iterable<Observable.Transformer<T, R>> transformers;

  MergedTransformer(Iterable<Observable.Transformer<T, R>> transformers) {
    this.transformers = transformers;
  }

  @Override
  public Observable<R> call(Observable<T> input) {
    return input.publish(
        new Func1<Observable<T>, Observable<R>>() {
          @Override
          public Observable<R> call(Observable<T> innerInput) {
            final List<Observable<R>> transformed = new ArrayList<>();
            for (Observable.Transformer<T, R> transformer : transformers) {
              transformed.add(innerInput.compose(transformer));
            }
            return Observable.merge(transformed);
          }
        });
  }
}
