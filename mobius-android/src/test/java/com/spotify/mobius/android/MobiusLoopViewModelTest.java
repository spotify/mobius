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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import com.spotify.mobius.Connectable;
import com.spotify.mobius.First;
import com.spotify.mobius.Mobius;
import com.spotify.mobius.Next;
import com.spotify.mobius.Update;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestEffect;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestEvent;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestModel;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestViewEffect;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.TestViewEffectHandler;
import com.spotify.mobius.android.MobiusLoopViewModelTestUtilClasses.ViewEffectSendingEffectHandler;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.ImmediateWorkRunner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MobiusLoopViewModelTest {

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
  private List<TestEvent> recordedEvents = new ArrayList<>();
  private final Update<TestModel, TestEvent, TestEffect> updateFunction =
      (model, event) -> {
        recordedEvents.add(event);
        return Next.noChange();
      };
  private MobiusLoopViewModel<TestModel, TestEvent, TestEffect, TestViewEffect> underTest;
  private TestViewEffectHandler<TestEvent, TestEffect, TestViewEffect> testViewEffectHandler;

  private FakeLifecycleOwner fakeLifecycle;
  private RecordingObserver<TestModel> recordingModelObserver = new RecordingObserver<>();
  private RecordingObserver<TestViewEffect> recordingForegroundViewEffectObserver =
      new RecordingObserver<>();
  private RecordingObserver<Iterable<TestViewEffect>> recordingBackgroundEffectObserver =
      new RecordingObserver<>();
  private TestModel initialModel;

  @Before
  public void setUp() {
    fakeLifecycle = new FakeLifecycleOwner();
    recordedEvents = new ArrayList<>();
    testViewEffectHandler = null;
    recordingModelObserver = new RecordingObserver<>();
    recordingForegroundViewEffectObserver = new RecordingObserver<>();
    recordingBackgroundEffectObserver = new RecordingObserver<>();
    initialModel = new TestModel("initial model");
    //noinspection Convert2MethodRef
    underTest =
        new MobiusLoopViewModel<>(
            (Consumer<TestViewEffect> consumer) -> {
              testViewEffectHandler = new TestViewEffectHandler<>(consumer);
              return Mobius.loop(updateFunction, testViewEffectHandler)
                  .eventRunner(ImmediateWorkRunner::new)
                  .effectRunner(ImmediateWorkRunner::new);
            },
            initialModel,
            (TestModel model) -> First.first(model),
            new ImmediateWorkRunner(),
            100);
    underTest.getModels().observe(fakeLifecycle, recordingModelObserver);
    underTest
        .getViewEffects()
        .setObserver(
            fakeLifecycle,
            recordingForegroundViewEffectObserver,
            recordingBackgroundEffectObserver);
  }

  @Test
  public void testViewModelgetModelAtStartIsInitialModel() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(underTest.getModel().name, equalTo("initial model"));
    recordingModelObserver.assertValues(initialModel);
  }

  @Test
  public void testViewModelSendsEffectsIntoLoop() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    underTest.dispatchEvent(new TestEvent("testable"));
    assertThat(recordedEvents.size(), equalTo(1));
    assertThat(recordedEvents.get(0).name, equalTo("testable"));
  }

  @Test
  public void testViewModelDoesNotSendViewEffectsIfLifecycleIsPaused() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    testViewEffectHandler.viewEffectConsumer.accept(new TestViewEffect("view effect 1"));
    assertThat(recordingForegroundViewEffectObserver.valueCount(), equalTo(0));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(0));
  }

  @Test
  public void
      testViewModelSendsViewEffectsToBackgroundObserverWhenLifecycleWasPausedThenIsResumed() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    testViewEffectHandler.viewEffectConsumer.accept(new TestViewEffect("view effect 1"));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(0));
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(1));
  }

  @Test
  public void testViewModelSendsViewEffectsToForegroundObserverWhenLifecycleIsResumed() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    testViewEffectHandler.viewEffectConsumer.accept(new TestViewEffect("view effect 1"));
    assertThat(recordingForegroundViewEffectObserver.valueCount(), equalTo(1));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(0));
  }

  @Test
  public void testViewModelDoesNotTryToForwardEventsIntoLoopAfterCleared() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    underTest.onCleared();
    underTest.dispatchEvent(new TestEvent("don't record me"));
    assertThat(recordedEvents.size(), equalTo(0));
  }

  @Test
  public void testViewEffectsPostedImmediatelyAreSentCorrectly() {
    //noinspection Convert2MethodRef
    underTest =
        new MobiusLoopViewModel<>(
            (Consumer<TestViewEffect> consumer) -> {
              Connectable<TestEffect, TestEvent> viewEffectSendingEffectHandler =
                  new ViewEffectSendingEffectHandler(consumer);
              testViewEffectHandler = new TestViewEffectHandler<>(consumer);
              return Mobius.loop(updateFunction, viewEffectSendingEffectHandler)
                  .eventRunner(ImmediateWorkRunner::new)
                  .effectRunner(ImmediateWorkRunner::new);
            },
            initialModel,
            (TestModel model) -> First.first(model, effects(new TestEffect("oops"))),
            new ImmediateWorkRunner(),
            100);
    underTest
        .getViewEffects()
        .setObserver(
            fakeLifecycle,
            recordingForegroundViewEffectObserver,
            recordingBackgroundEffectObserver);
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    assertThat(recordingForegroundViewEffectObserver.valueCount(), equalTo(0));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(1));
  }

  private Set<TestEffect> effects(TestEffect... effects) {
    final Set<TestEffect> result = new HashSet<>(effects.length);
    Collections.addAll(result, effects);
    return result;
  }
}
