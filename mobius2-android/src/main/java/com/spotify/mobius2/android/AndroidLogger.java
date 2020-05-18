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
package com.spotify.mobius2.android;

import static com.spotify.mobius2.internal_util.Preconditions.checkNotNull;

import android.util.Log;
import com.spotify.mobius2.First;
import com.spotify.mobius2.MobiusLoop;
import com.spotify.mobius2.Next;

public class AndroidLogger<M, E, F> implements MobiusLoop.Logger<M, E, F> {

  private final String tag;

  public static <M, E, F> AndroidLogger<M, E, F> tag(String tag) {
    return new AndroidLogger<>(tag);
  }

  public AndroidLogger(String tag) {
    this.tag = checkNotNull(tag);
  }

  @Override
  public void beforeInit(M model) {
    Log.d(tag, "Initializing loop");
  }

  @Override
  public void afterInit(M model, First<M, F> result) {
    Log.d(tag, "Loop initialized, starting from model: " + result.model());

    for (F effect : result.effects()) {
      Log.d(tag, "Effect dispatched: " + effect);
    }
  }

  @Override
  public void exceptionDuringInit(M model, Throwable exception) {
    Log.e(
        tag, "FATAL ERROR: exception during initialization from model '" + model + "'", exception);
  }

  @Override
  public void beforeUpdate(M model, E event) {
    Log.d(tag, "Event received: " + event);
  }

  @Override
  public void afterUpdate(M model, E event, Next<M, F> result) {
    if (result.hasModel()) {
      Log.d(tag, "Model updated: " + result.modelUnsafe());
    }

    for (F effect : result.effects()) {
      Log.d(tag, "Effect dispatched: " + effect);
    }
  }

  @Override
  public void exceptionDuringUpdate(M model, E event, Throwable exception) {
    Log.e(
        tag,
        String.format("FATAL ERROR: exception updating model '%s' with event '%s'", model, event),
        exception);
  }
}
