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

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.internal_util.ImmutableUtil;
import java.util.List;
import javax.annotation.Nonnull;

/** TODO: document! */
class UnknownEffectReportingConnectable<F, E> implements Connectable<F, E> {

  private final List<Class<?>> handledClasses;

  public UnknownEffectReportingConnectable(List<Class<?>> classes) {
    this.handledClasses = ImmutableUtil.immutableList(classes);
  }

  @Nonnull
  @Override
  public Connection<F> connect(Consumer<E> output) throws ConnectionLimitExceededException {
    return new Connection<F>() {
      @Override
      public void accept(F value) {
        for (Class<?> handledClass : handledClasses) {
          if (handledClass.isInstance(value)) {
            return;
          }
        }

        throw new UnknownEffectException(value);
      }

      @Override
      public void dispose() {}
    };
  }
}
