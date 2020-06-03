/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
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
package com.spotify.mobius.rx3;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Transformer that routes each incoming Effect descriptor to a sub-transformer associated with the
 * Effect descriptor class.
 */
class MobiusEffectRouter<F, E> implements ObservableTransformer<F, E> {

  @NonNull private final MergedTransformer<F, E> mergedTransformer;

  MobiusEffectRouter(
      @NonNull Set<Class<?>> handledEffectClasses,
      @NonNull Collection<ObservableTransformer<F, E>> effectPerformers) {

    final Set<Class<?>> effectClasses = new HashSet<>(handledEffectClasses);
    final List<ObservableTransformer<F, E>> immutableEffectPerformers =
        Collections.unmodifiableList(new ArrayList<>(effectPerformers));

    final ObservableTransformer<F, E> unhandledEffectHandler =
        new ObservableTransformer<F, E>() {
          @Override
          public @NonNull ObservableSource<E> apply(@NonNull Observable<F> effects) {
            return effects
                .filter(
                    new Predicate<F>() {
                      @Override
                      public boolean test(F e) throws Throwable {
                        for (Class<?> effectClass : effectClasses) {
                          if (effectClass.isAssignableFrom(e.getClass())) {
                            return false;
                          }
                        }
                        return true;
                      }
                    })
                .map(
                    new Function<F, E>() {
                      @Override
                      public E apply(F e) throws Throwable {
                        throw new UnknownEffectException(e);
                      }
                    });
          }
        };
    final List<ObservableTransformer<F, E>> allHandlers =
        new ArrayList<>(immutableEffectPerformers);
    allHandlers.add(unhandledEffectHandler);
    mergedTransformer = new MergedTransformer<>(allHandlers);
  }

  @Override
  public Observable<E> apply(@NonNull Observable<F> effects) {
    return effects.compose(mergedTransformer);
  }
}
