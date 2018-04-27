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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class EffectRouterBuilderImplTest {

  private TestConsumer<Event> eventConsumer;
  private AtomicBoolean ranSimpleEffect;

  private EffectRouterBuilderImpl<Effect, Event> builder;
  public static final Runnable DUMMY_ACTION =
      new Runnable() {
        @Override
        public void run() {}
      };

  @Before
  public void setUp() throws Exception {
    eventConsumer = new TestConsumer<>();
    ranSimpleEffect = new AtomicBoolean(false);
    builder = new EffectRouterBuilderImpl<>();
  }

  @Test
  public void integrationTest() throws Exception {
    final AtomicReference<String> ewpReference = new AtomicReference<>();
    final Event event = new Event();

    Connection<Effect> connection =
        Mobius.<Effect, Event>subtypeEffectHandler()
            .addConnectable(EffectWithEvent.class, eventEmitter(event))
            .addRunnable(
                SimpleEffect.class,
                new Runnable() {
                  @Override
                  public void run() {
                    ranSimpleEffect.set(true);
                  }
                })
            .addConsumer(
                EffectWithParameter.class,
                new Consumer<EffectWithParameter>() {
                  @Override
                  public void accept(EffectWithParameter value) {
                    ewpReference.set(value.param);
                  }
                })
            .build()
            .connect(eventConsumer);

    connection.accept(new EffectWithEvent());
    assertThat(eventConsumer.received).containsExactly(event);

    connection.accept(new SimpleEffect());
    assertThat(ranSimpleEffect.get()).isTrue();

    connection.accept(new EffectWithParameter("heya froobish"));
    assertThat(ewpReference.get()).isEqualTo("heya froobish");
  }

  @Test
  public void shouldSupportRunnables() throws Exception {
    verifySimpleEffectExecution(
        builder -> builder.addRunnable(SimpleEffect.class, () -> ranSimpleEffect.set(true)));
  }

  @Test
  public void shouldSupportConsumers() throws Exception {
    verifySimpleEffectExecution(
        builder -> builder.addConsumer(SimpleEffect.class, value -> ranSimpleEffect.set(true)));
  }

  @Test
  public void shouldSupportFunctions() throws Exception {
    verifySimpleEffectExecution(
        builder -> {
          return builder.addFunction(
              SimpleEffect.class,
              value -> {
                ranSimpleEffect.set(true);
                return new Event();
              });
        });
  }

  @Test
  public void shouldSupportConnectables() throws Exception {
    verifySimpleEffectExecution(
        builder ->
            builder.addConnectable(
                SimpleEffect.class,
                new Connectable<SimpleEffect, Event>() {
                  @Nonnull
                  @Override
                  public Connection<SimpleEffect> connect(Consumer<Event> output)
                      throws ConnectionLimitExceededException {
                    return new Connection<SimpleEffect>() {
                      @Override
                      public void accept(SimpleEffect value) {
                        ranSimpleEffect.set(true);
                      }

                      @Override
                      public void dispose() {}
                    };
                  }
                }));
  }

  @Test
  public void shouldPreventSubclassCollisionsAtConfigTime() throws Exception {
    builder.addRunnable(SimpleEffect.class, DUMMY_ACTION);

    assertThatThrownBy(() -> builder.addRunnable(SubEffect.class, DUMMY_ACTION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(SimpleEffect.class.getName())
        .hasMessageContaining(SubEffect.class.getName());
  }

  @Test
  public void shouldPreventSameClassCollisionsAtConfigTime() throws Exception {
    builder.addRunnable(SimpleEffect.class, DUMMY_ACTION);

    assertThatThrownBy(() -> builder.addRunnable(SimpleEffect.class, DUMMY_ACTION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(SimpleEffect.class.getName());
  }

  @Test
  public void shouldPreventSuperClassCollisionsAtConfigTime() throws Exception {
    builder.addRunnable(SubEffect.class, DUMMY_ACTION);

    assertThatThrownBy(() -> builder.addRunnable(SimpleEffect.class, DUMMY_ACTION))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(SimpleEffect.class.getName())
        .hasMessageContaining(SubEffect.class.getName());
  }

  // TODO: maybe separate router tests from builder tests?
  @Test
  public void shouldReportUnhandledEffectsAtRuntime() throws Exception {
    final Connection<Effect> connection =
        builder.addRunnable(SimpleEffect.class, DUMMY_ACTION).build().connect(eventConsumer);

    assertThatThrownBy(() -> connection.accept(new UnhandledEffect()))
        .isInstanceOf(UnknownEffectException.class);
  }

  @Test
  public void shouldNeverSendEventsAfterDispose() throws Exception {
    Connection<Effect> connection =
        builder
            .addConnectable(EffectWithEvent.class, eventEmitter(new Event()))
            .build()
            .connect(eventConsumer);

    connection.accept(new EffectWithEvent());

    connection.dispose();

    connection.accept(new EffectWithEvent());

    assertThat(eventConsumer.received).hasSize(1);
  }

  private void verifySimpleEffectExecution(
      Function<EffectRouterBuilder<Effect, Event>, EffectRouterBuilder<Effect, Event>>
          buildConfig) {
    final EffectRouterBuilder<Effect, Event> builder =
        buildConfig.apply(Mobius.<Effect, Event>subtypeEffectHandler());

    builder.build().connect(eventConsumer).accept(new SimpleEffect());

    assertThat(ranSimpleEffect.get()).isTrue();
  }

  private Connectable<EffectWithEvent, Event> eventEmitter(final Event event) {
    return new Connectable<EffectWithEvent, Event>() {
      @Nonnull
      @Override
      public Connection<EffectWithEvent> connect(final Consumer<Event> output)
          throws ConnectionLimitExceededException {
        return new Connection<EffectWithEvent>() {
          @Override
          public void accept(EffectWithEvent value) {
            output.accept(event);
          }

          @Override
          public void dispose() {}
        };
      }
    };
  }

  private static class Effect {}

  private static class SimpleEffect extends Effect {}

  private static class SubEffect extends SimpleEffect {}

  private static class EffectWithParameter extends Effect {
    final String param;

    private EffectWithParameter(String param) {
      this.param = param;
    }
  }

  private static class EffectWithEvent extends Effect {}

  private static class UnhandledEffect extends Effect {}

  private static class Event {}
}
