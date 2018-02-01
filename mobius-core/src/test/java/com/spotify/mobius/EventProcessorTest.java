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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.Sets;
import com.spotify.mobius.internal_util.ImmutableUtil;
import com.spotify.mobius.test.RecordingConsumer;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class EventProcessorTest {

  private EventProcessor<String, Integer, Long> underTest;
  private RecordingConsumer<Long> effectConsumer;
  private RecordingConsumer<String> stateConsumer;

  @Before
  public void setUp() throws Exception {
    effectConsumer = new RecordingConsumer<>();
    stateConsumer = new RecordingConsumer<>();
    underTest = new EventProcessor<>(createStore(), effectConsumer, stateConsumer);
    underTest.init();
  }

  @Test
  public void shouldEmitStateIfStateChanged() throws Exception {
    underTest.update(1);
    stateConsumer.assertValues("init!", "init!->1");
  }

  @Test
  public void shouldNotEmitStateIfStateNotChanged() throws Exception {
    stateConsumer.clearValues();
    underTest.update(0);
    stateConsumer.assertValues();
  }

  @Test
  public void shouldOnlyEmitStateStateChanged() throws Exception {
    underTest.update(0);
    underTest.update(1);
    underTest.update(0);
    underTest.update(2);
    stateConsumer.assertValues("init!", "init!->1", "init!->1->2");
  }

  @Test
  public void shouldEmitEffectsWhenStateChanges() throws Exception {
    effectConsumer.clearValues();
    underTest.update(3);
    effectConsumer.assertValuesInAnyOrder(10L, 20L, 30L);
  }

  @Test
  public void shouldEmitStateDuringInit() throws Exception {
    stateConsumer.assertValues("init!");
  }

  @Test
  public void shouldEmitEffectsDuringInit() throws Exception {
    effectConsumer.assertValuesInAnyOrder(15L, 25L, 35L);
  }

  @Test
  public void shouldQueueUpdatesReceivedBeforeInit() throws Exception {
    stateConsumer.clearValues();
    underTest = new EventProcessor<>(createStore(), effectConsumer, stateConsumer);

    underTest.update(1);
    underTest.update(2);
    underTest.update(3);

    underTest.init();

    stateConsumer.assertValues("init!", "init!->1", "init!->1->2", "init!->1->2->3");
  }

  @Test
  public void shouldDisallowDuplicateInitialisation() throws Exception {
    assertThatThrownBy(() -> underTest.init()).isInstanceOf(IllegalStateException.class);
  }

  private MobiusStore<String, Integer, Long> createStore() {
    return MobiusStore.create(
        new Init<String, Long>() {
          @Nonnull
          @Override
          public First<String, Long> init(String model) {
            return First.first(model + "!", ImmutableUtil.setOf(15L, 25L, 35L));
          }
        },
        new Update<String, Integer, Long>() {
          @Nonnull
          @Override
          public Next<String, Long> update(String model, Integer event) {
            if (event == 0) {
              return Next.noChange();
            }

            Set<Long> effects = Sets.newHashSet();
            for (int i = 0; i < event; i++) {
              effects.add(10L * (i + 1));
            }

            return Next.next(model + "->" + event, effects);
          }
        },
        "init");
  }
}
