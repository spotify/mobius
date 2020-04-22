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
package com.spotify.mobius.rx2;

import com.spotify.mobius.ConnectionException;

/**
 * Helper exception type that enables capturing the correct class and method for exceptions
 * happening in {@link io.reactivex.ObservableTransformer}s.
 */
class EffectHandlerException extends ConnectionException {

  private EffectHandlerException(Throwable throwable) {
    super("Error in effect handler", throwable);
  }

  public static EffectHandlerException in(Object effectHandler, Throwable cause) {
    EffectHandlerException e = new EffectHandlerException(cause);

    final StackTraceElement[] stackTrace = e.getStackTrace();

    // add a synthetic StackTraceElement so that the effect handler class name will be reported in
    // the exception. This helps troubleshooting where the issue originated from.
    stackTrace[0] = new StackTraceElement(effectHandler.getClass().getName(), "apply", null, -1);

    e.setStackTrace(stackTrace);

    return e;
  }
}
