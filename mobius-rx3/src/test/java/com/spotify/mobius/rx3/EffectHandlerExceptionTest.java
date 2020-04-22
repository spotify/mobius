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
package com.spotify.mobius.rx3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class EffectHandlerExceptionTest {

  private static class PretendEffectHandler {}

  @Test
  public void shouldProvideAGoodStackTrace() throws Exception {
    final RuntimeException cause = new RuntimeException("hey");

    assertThatThrownBy(
            () -> {
              throw EffectHandlerException.in(new PretendEffectHandler(), cause);
            })
        .hasStackTraceContaining(PretendEffectHandler.class.getName())
        .hasCause(cause);
  }
}
