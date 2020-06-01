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
package com.spotify.mobius2.extras;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;

import com.spotify.mobius2.MobiusLoop;
import com.spotify.mobius2.functions.BiFunction;
import java.time.Duration;
import javax.annotation.Nonnull;
import org.junit.Test;

public class MobiusExtrasTest {

  private static final BiFunction<String, Integer, String> BEGINNER_UPDATE =
      new BiFunction<String, Integer, String>() {
        @Nonnull
        @Override
        public String apply(String model, Integer event) {
          return model + String.valueOf(event);
        }
      };

  private static final String MY_MODEL = "start";

  @Test
  public void shouldInstantiateBeginnerWithMinimumParams() throws Exception {
    MobiusLoop<String, Integer, ?> loop =
        MobiusExtras.beginnerLoop(BEGINNER_UPDATE).startFrom(MY_MODEL);

    loop.dispatchEvent(8);

    await().atMost(Duration.ofSeconds(1)).until(() -> loop.getMostRecentModel(), is("start8"));
  }
}
