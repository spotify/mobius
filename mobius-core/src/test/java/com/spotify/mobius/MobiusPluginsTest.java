package com.spotify.mobius;

import static com.spotify.mobius.Effects.effects;
import static org.assertj.core.api.Assertions.assertThat;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Producer;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.test.TestWorkRunner;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Test;

public class MobiusPluginsTest {
  private boolean wasEffectRun = false;
  private boolean wasEventRun = false;

  private final Connectable<String, String> effectHandler =
      eventConsumer ->
          new Connection<>() {
            @Override
            public void accept(String value) {
              wasEffectRun = true;
            }

            @Override
            public void dispose() {}
          };

  private final Update<String, String, String> update =
      (String model, String event) -> {
        wasEventRun = true;
        return Next.dispatch(effects("effect"));
      };

  @After
  public void tearDown() {
    MobiusPlugins.setDefaultEventRunner(null);
    MobiusPlugins.setDefaultEffectRunner(null);
  }

  @Test
  public void shouldUseDefaultEventAndEffectRunners() {
    TestWorkRunner testEffectWorkRunner = new TestWorkRunner();
    TestWorkRunner testEventWorkRunner = new TestWorkRunner();
    MobiusPlugins.setDefaultEffectRunner(() -> testEffectWorkRunner);
    MobiusPlugins.setDefaultEventRunner(() -> testEventWorkRunner);

    MobiusLoop<String, String, String> loop =
        Mobius.loop(update, effectHandler).startFrom("init-model");
    RecordingModelObserver<String> observer = new RecordingModelObserver<>();
    Disposable unregister = loop.observe(observer);

    loop.dispatchEvent("event");

    assertThat(wasEventRun).isFalse();
    assertThat(wasEffectRun).isFalse();

    testEventWorkRunner.runAll();
    testEffectWorkRunner.runAll();

    assertThat(wasEventRun).isTrue();
    assertThat(wasEffectRun).isTrue();

    unregister.dispose();
    loop.dispose();
  }

  @Test
  public void shouldDisposeDefaultEventAndEffectRunners() {
    TestWorkRunner testEffectWorkRunner = new TestWorkRunner();
    TestWorkRunner testEventWorkRunner = new TestWorkRunner();
    MobiusPlugins.setDefaultEffectRunner(() -> testEffectWorkRunner);
    MobiusPlugins.setDefaultEventRunner(() -> testEventWorkRunner);

    Mobius.loop(update, effectHandler).startFrom("init-model").dispose();

    assertThat(testEffectWorkRunner.isDisposed()).isTrue();
    assertThat(testEventWorkRunner.isDisposed()).isTrue();
  }

  @Test
  public void shouldUseNewWorkerForEachLoop() {
    TestProducer<TestWorkRunner> effectRunnerProducer = new TestProducer<>(TestWorkRunner::new);
    TestProducer<TestWorkRunner> eventRunnerProducer = new TestProducer<>(TestWorkRunner::new);
    MobiusPlugins.setDefaultEffectRunner(effectRunnerProducer::get);
    MobiusPlugins.setDefaultEventRunner(eventRunnerProducer::get);

    Mobius.loop(update, effectHandler).startFrom("init-model").dispose();
    assertThat(effectRunnerProducer.producedItems).hasSize(1);
    assertThat(eventRunnerProducer.producedItems).hasSize(1);

    Mobius.loop(update, effectHandler).startFrom("init-model").dispose();
    assertThat(effectRunnerProducer.producedItems).hasSize(2);
    assertThat(eventRunnerProducer.producedItems).hasSize(2);

    assertThat(effectRunnerProducer.producedItems.stream().map(TestWorkRunner::isDisposed))
        .containsExactly(true, true);
    assertThat(eventRunnerProducer.producedItems.stream().map(TestWorkRunner::isDisposed))
        .containsExactly(true, true);
  }

  private static class TestProducer<T> implements Producer<T> {

    private final Producer<T> originalProducer;
    public List<T> producedItems = new ArrayList<>();

    public TestProducer(Producer<T> originalProducer) {

      this.originalProducer = originalProducer;
    }

    @Nonnull
    @Override
    public T get() {
      T produced = originalProducer.get();
      producedItems.add(produced);
      return produced;
    }
  }
}
