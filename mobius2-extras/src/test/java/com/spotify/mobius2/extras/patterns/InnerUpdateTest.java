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
package com.spotify.mobius2.extras.patterns;

import static com.spotify.mobius2.Effects.effects;
import static com.spotify.mobius2.Next.dispatch;
import static com.spotify.mobius2.Next.noChange;
import static com.spotify.mobius2.extras.patterns.InnerEffectHandlers.ignoreEffects;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.spotify.mobius2.Next;
import com.spotify.mobius2.test.NextMatchers;
import org.junit.Test;

public class InnerUpdateTest {

  @Test
  public void canExtractInnerModel() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> "extracted_model")
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> Next.next(model))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler(ignoreEffects())
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("extracted_model"));
  }

  @Test
  public void canExtractInnerEvent() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> "extracted_event")
            .innerUpdate((model, event) -> Next.next(event))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler(ignoreEffects())
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("extracted_event"));
  }

  @Test
  public void cannotExtractNullModel() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> null)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> Next.next(model))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler(ignoreEffects())
            .build();

    try {
      innerUpdate.update("model", "event");
      fail("did not throw null pointer exception");
    } catch (NullPointerException e) {
      // Success.
    }
  }

  @Test
  public void cannotExtractNullEvent() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> null)
            .innerUpdate((model, event) -> Next.next(model))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler(ignoreEffects())
            .build();

    try {
      innerUpdate.update("model", "event");
      fail("did not throw null pointer exception");
    } catch (NullPointerException e) {
      // Success.
    }
  }

  @Test
  public void callsInnerUpdate() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> Next.next("inner_update"))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler(ignoreEffects())
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("inner_update"));
  }

  @Test
  public void innerUpdateCannotReturnNull() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> null)
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler(ignoreEffects())
            .build();

    try {
      innerUpdate.update("model", "event");
      fail("did not throw null pointer exception");
    } catch (NullPointerException e) {
      // Success.
    }
  }

  @Test
  public void noChangeDoesNotCallModelUpdater() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> noChange())
            .modelUpdater((m, mi) -> "model_updater")
            .innerEffectHandler(ignoreEffects())
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasNothing());
  }

  @Test
  public void updatedModelCallsModelUpdater() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> Next.next("inner_update"))
            .modelUpdater((m, mi) -> "model_updater")
            .innerEffectHandler(ignoreEffects())
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("model_updater"));
  }

  @Test
  public void dispatchEffectCallsInnerEffectHandler() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> dispatch(effects("1", "2", "3")))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler((model, updated, effects) -> Next.next("effect_handler"))
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("effect_handler"));
  }

  @Test
  public void noEffectsStillCallsInnerEffectHandler() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> Next.next("inner_update"))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler((model, updated, effects) -> Next.next("effect_handler"))
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("effect_handler"));
    assertThat(next, NextMatchers.<String, String>hasNoEffects());
  }

  @Test
  public void noChangeNoEffectsStillCallsInnerEffectHandler() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> noChange())
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler((model, updated, effects) -> Next.next("effect_handler"))
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("effect_handler"));
    assertThat(next, NextMatchers.<String, String>hasNoEffects());
  }

  @Test
  public void updatedModelNoEffectsStillCallsInnerEffectHandler() throws Exception {
    InnerUpdate<String, String, String, String, String, String> innerUpdate =
        InnerUpdate.<String, String, String, String, String, String>builder()
            .modelExtractor(m -> m)
            .eventExtractor(e -> e)
            .innerUpdate((model, event) -> Next.next("inner_update"))
            .modelUpdater((m, mi) -> mi)
            .innerEffectHandler((model, updated, effects) -> Next.next("effect_handler"))
            .build();

    Next<String, String> next = innerUpdate.update("model", "event");

    assertThat(next, NextMatchers.<String, String>hasModel("effect_handler"));
    assertThat(next, NextMatchers.<String, String>hasNoEffects());
  }
}
