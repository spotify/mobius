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
package com.spotify.mobius.extras;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.EventSource;
import com.spotify.mobius.disposables.Disposable;
import com.spotify.mobius.functions.Consumer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An event source that forwards the emitted values of a target Event Source, but whose emissions
 * are toggled on/off by a secondary Boolean-emitting Event Source
 *
 * @param <T> The Event class
 */
public final class ToggledEventSource<T> implements EventSource<T> {
  @Nonnull private final EventSource<T> targetEventSource;
  @Nonnull private final List<Consumer<T>> consumers = new ArrayList<>(1);
  @Nullable private Disposable disposable = null;
  private Boolean active;

  /**
   * Returns a new EventSource that re-emits events from the <code>targetEventSource</code>
   * depending on the value emitted by the <code>togglingEventSource</code><br>
   * When the state is set to not re-emit values, this Event Source unsubscribed from the <code>
   * targetEventSource</code> completely, and re-subscribes when the state is set to emit values
   * again.
   *
   * @param <E> The class of the events being re-emitted
   * @param targetEventSource The Event Source whose events will be conditionally re-emitted
   * @param togglingSource The Event Source whose emitted values control whether the <code>
   *     targetEventSource</code>'s values are re-emitted (when togglingSource emits True) or
   *     discarded (when goggling Source emits False)
   * @param initialToggleState The initial state of whether to re-emit values or not. True to allow
   *     re-emitting, false to stop it.
   */
  @Nonnull
  public static <E> EventSource<E> from(
      @Nonnull EventSource<E> targetEventSource,
      @Nonnull EventSource<Boolean> togglingSource,
      boolean initialToggleState) {
    final ToggledEventSource<E> toggledEventSource =
        new ToggledEventSource<>(targetEventSource, initialToggleState);
    new ToggledEventSourceWeakReferenceConnection(
        togglingSource, new WeakReference<>(toggledEventSource));
    return toggledEventSource;
  }

  private ToggledEventSource(@Nonnull EventSource<T> targetEventSource, boolean isActive) {
    this.targetEventSource = checkNotNull(targetEventSource);
    this.active = isActive;
  }

  private void setActive(Boolean active) {
    this.active = active;
    checkToUpdateSubscription();
  }

  private void checkToUpdateSubscription() {
    synchronized (consumers) {
      if (!active || consumers.size() == 0) {
        if (disposable != null) {
          disposable.dispose();
        }
        disposable = null;
      } else {
        if (disposable == null) {
          disposable = targetEventSource.subscribe(this::onValueEmitted);
        }
      }
    }
  }

  private void onValueEmitted(T f) {
    final List<Consumer<T>> toSendTo;
    synchronized (consumers) {
      toSendTo = new ArrayList<>(consumers);
    }
    for (Consumer<T> consumer : toSendTo) {
      consumer.accept(f);
    }
  }

  @Nonnull
  @Override
  public Disposable subscribe(Consumer<T> eventConsumer) {
    synchronized (consumers) {
      consumers.add(eventConsumer);
      checkToUpdateSubscription();
    }
    return new Disposer<>(this, eventConsumer);
  }

  private void dispose(Consumer<T> eventConsumer) {
    synchronized (consumers) {
      consumers.remove(eventConsumer);
      checkToUpdateSubscription();
    }
  }

  private static class Disposer<T> implements Disposable {
    private ToggledEventSource<T> eventSource;
    private Consumer<T> eventConsumer;

    private Disposer(ToggledEventSource<T> eventSource, Consumer<T> eventConsumer) {

      this.eventSource = eventSource;
      this.eventConsumer = eventConsumer;
    }

    @Override
    public void dispose() {
      if (eventSource != null) {
        eventSource.dispose(eventConsumer);
        eventSource = null;
        eventConsumer = null;
      }
    }
  }

  /**
   * This class observes the given <code>togglingSource</code> and uses its state to set the given
   * <code>ToggledEventSource</code> as either active (forwarding events) or inactive (not
   * forwarding events). This class only keeps a Weak Reference to the <code>ToggledEventSource
   * </code>, and thus will not keep the object alive, and the reference is nulled, this object will
   * unsubscribe from the <code>togglingSource</code>.
   */
  private static class ToggledEventSourceWeakReferenceConnection implements Consumer<Boolean> {
    private final WeakReference<ToggledEventSource<?>> toggledEventSourceRef;
    private final Disposable togglingEventSourceDisposable;

    ToggledEventSourceWeakReferenceConnection(
        EventSource<Boolean> togglingSource,
        WeakReference<ToggledEventSource<?>> toggledEventSourceWeakReference) {
      toggledEventSourceRef = toggledEventSourceWeakReference;
      togglingEventSourceDisposable = togglingSource.subscribe(this);
    }

    @Override
    public void accept(Boolean value) {
      final ToggledEventSource<?> toggledEventSource = toggledEventSourceRef.get();
      if (toggledEventSource != null) {
        toggledEventSource.setActive(value);
      } else {
        togglingEventSourceDisposable.dispose();
      }
    }
  }
}
