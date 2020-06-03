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
package com.spotify.mobius.android;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.ConnectionLimitExceededException;
import com.spotify.mobius.functions.Consumer;
import javax.annotation.Nonnull;

public class MobiusLoopViewModelTestUtilClasses {
  private MobiusLoopViewModelTestUtilClasses() {}

  static class TestEvent {
    final String name;

    TestEvent(String name) {
      this.name = name;
    }
  }

  static class TestEffect {
    final String name;

    TestEffect(String name) {
      this.name = name;
    }
  }

  static class TestModel {
    final String name;

    TestModel(String name) {
      this.name = name;
    }
  }

  static class TestViewEffect {
    final String name;

    TestViewEffect(String name) {
      this.name = name;
    }
  }

  static class TestViewEffectHandler<E, F, V> implements Connectable<F, E> {
    final Consumer<V> viewEffectConsumer;
    private volatile Consumer<E> eventConsumer = null;

    TestViewEffectHandler(Consumer<V> viewEffectConsumer) {
      this.viewEffectConsumer = viewEffectConsumer;
    }

    public void sendEvent(E event) {
      eventConsumer.accept(event);
    }

    @Nonnull
    @Override
    public Connection<F> connect(Consumer<E> output) throws ConnectionLimitExceededException {
      if (eventConsumer != null) {
        throw new ConnectionLimitExceededException();
      }

      eventConsumer = output;

      return new Connection<F>() {
        @Override
        public void accept(F value) {
          // do nothing
        }

        @Override
        public void dispose() {
          // do nothing
        }
      };
    }
  }
}
