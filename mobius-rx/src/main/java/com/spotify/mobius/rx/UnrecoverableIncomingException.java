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

/**
 * Used to indicate that an {@link RxMobiusLoop} transformer has received an {@link
 * rx.Observer#onError(Throwable)} call, which is illegal. This exception means Mobius is in an
 * undefined state and should be considered a fatal programmer error. Do not try to handle this
 * exception in your code, ensure it never gets thrown.
 */
class UnrecoverableIncomingException extends RuntimeException {

  UnrecoverableIncomingException(Throwable throwable) {
    super(
        "PROGRAMMER ERROR: Mobius cannot recover from this exception; ensure your event sources don't invoke onError",
        throwable);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UnrecoverableIncomingException)) {
      return false;
    }

    return o == this || getCause().equals(((UnrecoverableIncomingException) o).getCause());
  }
}
