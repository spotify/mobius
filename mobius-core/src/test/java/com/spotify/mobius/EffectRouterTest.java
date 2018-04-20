package com.spotify.mobius;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.spotify.mobius.functions.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;

public class EffectRouterTest {

  private Connectable<Effect, Event> connectable;
  private TestConsumer<Event> eventConsumer;

  @Before
  public void setUp() throws Exception {
    eventConsumer = new TestConsumer<>();
  }

  @Test
  public void shouldFroobish() throws Exception {
    final AtomicBoolean ranSimpleEffect = new AtomicBoolean(false);
    final AtomicReference<EffectWithParameter> ewpReference = new AtomicReference<>();
    final AtomicReference<EffectWithEvent> eweReference = new AtomicReference<>();

    connectable = Mobius.<Effect, Event>subtypeEffectHandler()
        .add(EffectWithEvent.class, new Connectable<EffectWithEvent, Event>() {
          @Nonnull
          @Override
          public Connection<EffectWithEvent> connect(final Consumer<Event> output)
              throws ConnectionLimitExceededException {
            return new Connection<EffectWithEvent>() {
              @Override
              public void accept(EffectWithEvent value) {
                output.accept(new Event());
              }

              @Override
              public void dispose() {

              }
            };
          }
        })
        .add(SimpleEffect.class, new Runnable() {
          @Override
          public void run() {
            ranSimpleEffect.set(true);
          }
        })
        .add(EffectWithParameter.class, new Consumer<EffectWithParameter>() {
          @Override
          public void accept(EffectWithParameter value) {
            ewpReference.set(value);
          }
        })
        .build();
  }

  @Test
  public void shouldSupportRunnables() throws Exception {
    final AtomicBoolean ranSimpleEffect = new AtomicBoolean(false);

    connectable = Mobius.<Effect, Event>subtypeEffectHandler()
        .add(SimpleEffect.class, new Runnable() {
          @Override
          public void run() {
            ranSimpleEffect.set(true);
          }
        }).build();

    connectable.connect(eventConsumer).accept(new SimpleEffect());

    assertThat(ranSimpleEffect.get()).isTrue();
  }

  @Test
  public void shouldSupportConsumers() throws Exception {
    fail("not implemented");
  }

  @Test
  public void shouldSupportConnectables() throws Exception {
    fail("not implemented");
  }

  @Test
  public void shouldPreventSubclassCollisionsAtConfigTime() throws Exception {
    fail("not implemented");
  }

  @Test
  public void shouldPreventSameClassCollisionsAtConfigTime() throws Exception {
    fail("not implemented");
  }

  @Test
  public void shouldPreventSuperClassCollisionsAtConfigTime() throws Exception {
    fail("not implemented");
  }

  // TODO: this is
  @Test
  public void shouldReportUnhandledEffectsAtRuntime() throws Exception {
    fail("not implemented");
  }

  private static class Effect {

  }

  private static class SimpleEffect extends Effect {

  }

  private static class SubEffect extends SimpleEffect {}

  private static class EffectWithParameter extends Effect {

  }

  private static class EffectWithEvent extends Effect {

  }

  private static class UnhandledEffect extends Effect {

  }



  private static class Event {

  }
}