package com.spotify.mobius;

import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.test.RecordingModelObserver;
import com.spotify.mobius.testdomain.TestEvent;
import org.junit.Test;

public class MobiusLoopObservabilityBehavior extends MobiusLoopTest {
  @Test
  public void shouldTransitionToNextStateBasedOnInput() throws Exception {
    mobiusLoop.dispatchEvent(new TestEvent("first"));
    mobiusLoop.dispatchEvent(new TestEvent("second"));

    observer.assertStates("init", "init->first", "init->first->second");
  }

  @Test
  public void shouldSupportUnregisteringObserver() throws Exception {
    observer = new RecordingModelObserver<>();

    mobiusLoop =
        MobiusLoop.create(
            mobiusStore, effectHandler, eventSource, immediateRunner, immediateRunner);

    Disposable unregister = mobiusLoop.observe(observer);

    mobiusLoop.dispatchEvent(new TestEvent("active observer"));
    unregister.dispose();
    mobiusLoop.dispatchEvent(new TestEvent("shouldn't be seen"));

    observer.assertStates("init", "init->active observer");
  }
}
