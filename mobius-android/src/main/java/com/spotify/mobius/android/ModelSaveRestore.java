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
package com.spotify.mobius.android;

import android.os.Bundle;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** An interface that defines how to save and restore a Model to and from a Bundle. */
public interface ModelSaveRestore<M> {

  /**
   * This method will be called if there isn't any model available to be restored. Typically this is
   * because the view was started for the first time, but it could also be because {@link
   * #restoreModel(Bundle)} failed to restore a model.
   *
   * @return the default model to use if there's no model that can be restored
   */
  @Nonnull
  M getDefaultModel();

  /**
   * This method will be called with the current model and the bundle provided by Android to save
   * the model. This is where you save the model for state restoration.
   *
   * @param model the current model
   * @param out the bundle provided by Android to save things into
   */
  void saveModel(M model, Bundle out);

  /**
   * This method will be called to extract a saved model from the provided {@link Bundle}.
   *
   * <p>Returning null means that it wasn't possible to restore any model from the bundle.
   *
   * @param in the bundle containing data to be restored
   * @return the model that was restored from the bundle, or null if it wasn't possible to restore
   */
  @Nullable
  M restoreModel(Bundle in);
}
