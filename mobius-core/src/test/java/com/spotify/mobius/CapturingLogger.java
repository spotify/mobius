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
package com.spotify.mobius;

import com.google.auto.value.AutoValue;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class CapturingLogger<M, E, F> implements MobiusLoop.Logger<M, E, F> {

  final List<M> beforeInit = new CopyOnWriteArrayList<>();
  final List<AfterInitArgs<M, F>> afterInit = new CopyOnWriteArrayList<>();
  final List<InitErrorArgs<M>> initErrors = new CopyOnWriteArrayList<>();
  final List<BeforeUpdateArgs<M, E>> beforeUpdate = new CopyOnWriteArrayList<>();
  final List<AfterUpdateArgs<M, E, F>> afterUpdate = new CopyOnWriteArrayList<>();
  final List<UpdateErrorArgs<M, E>> updateErrors = new CopyOnWriteArrayList<>();

  @Override
  public void beforeInit(M model) {
    beforeInit.add(model);
  }

  @Override
  public void afterInit(M model, First<M, F> result) {
    afterInit.add(AfterInitArgs.create(model, result));
  }

  @Override
  public void exceptionDuringInit(M model, Throwable exception) {
    initErrors.add(InitErrorArgs.create(model, exception));
  }

  @Override
  public void beforeUpdate(M model, E event) {
    beforeUpdate.add(BeforeUpdateArgs.create(model, event));
  }

  @Override
  public void afterUpdate(M model, E event, Next<M, F> result) {
    afterUpdate.add(AfterUpdateArgs.create(model, event, result));
  }

  @Override
  public void exceptionDuringUpdate(M model, E event, Throwable exception) {
    updateErrors.add(UpdateErrorArgs.create(model, event, exception));
  }

  @AutoValue
  abstract static class AfterInitArgs<M, F> {

    abstract M model();

    abstract First<M, F> first();

    public static <M, F> AfterInitArgs<M, F> create(M model, First<M, F> first) {
      return new AutoValue_CapturingLogger_AfterInitArgs<>(model, first);
    }
  }

  @AutoValue
  abstract static class InitErrorArgs<M> {

    abstract M model();

    abstract Throwable exception();

    public static <M, F> InitErrorArgs<M> create(M model, Throwable exception) {
      return new AutoValue_CapturingLogger_InitErrorArgs<>(model, exception);
    }
  }

  @AutoValue
  abstract static class BeforeUpdateArgs<M, E> {

    abstract M model();

    abstract E event();

    public static <M, E> BeforeUpdateArgs<M, E> create(M model, E event) {
      return new AutoValue_CapturingLogger_BeforeUpdateArgs<>(model, event);
    }
  }

  @AutoValue
  abstract static class AfterUpdateArgs<M, E, F> {

    abstract M model();

    abstract E event();

    abstract Next<M, F> next();

    public static <M, E, F> AfterUpdateArgs<M, E, F> create(M model, E event, Next<M, F> next) {
      return new AutoValue_CapturingLogger_AfterUpdateArgs<>(model, event, next);
    }
  }

  @AutoValue
  abstract static class UpdateErrorArgs<M, E> {

    abstract M model();

    abstract E event();

    abstract Throwable exception();

    public static <M, E> UpdateErrorArgs<M, E> create(M model, E event, Throwable exception) {
      return new AutoValue_CapturingLogger_UpdateErrorArgs<>(model, event, exception);
    }
  }
}
