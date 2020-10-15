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
package com.spotify.mobius.rx2;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.istack.internal.NotNull;
import io.reactivex.plugins.RxJavaPlugins;
import java.util.ArrayList;
import java.util.List;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** A rule catching errors by setting an error handler with {@link RxJavaPlugins}. */
public class RxErrorsRule extends TestWatcher {

  private final List<Throwable> mErrors = new ArrayList<>(0);

  @Override
  protected void starting(@NotNull Description description) {
    RxJavaPlugins.setErrorHandler(mErrors::add);
  }

  @Override
  protected void finished(@NotNull Description description) {
    RxJavaPlugins.setErrorHandler(null);
  }

  public void assertSingleErrorWithCauseMessage(@NotNull String message) {
    assertThat(mErrors).hasSize(1);
    assertThat(mErrors.get(0).getCause().getMessage()).isEqualTo(message);
  }
}
