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
package com.spotify.mobius.android;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import com.spotify.mobius.First;
import com.spotify.mobius.Init;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.MobiusLoop.Factory;
import com.spotify.mobius.android.runners.MainThreadWorkRunner;
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import com.spotify.mobius.runners.WorkRunner;
import javax.annotation.Nonnull;

/**
 * A Mobius Loop lifecycle handler which is based on the Android ViewModel. <br>
 *
 * <p>This view model has the concept of a View Effect (parameter V) which is a type of effect that
 * requires the corresponding Android lifecycle owner to be in an active state i.e. between onResume
 * and onPause. To allow the normal effect handler to send these, the view model will provide a
 * Consumer of these View Effects to the Loop Factory Provider, which can then be passed into the
 * normal Effect handler so it can delegate view effects where necessary<br>
 *
 * <p>Since it's based on Android View model, this view model will keep the loop alive as long as
 * the lifecycle owner it is associated with (via a factory to produce it) is not destroyed -
 * meaning the Mobius loop will persist through rotations and brief app minimization to background,
 * <br>
 *
 * <p>While the loop is running but the view is paused, which is between onPause and onDestroy, the
 * view model will keep the latest model/state sent by the loop and will keep a queue of View
 * Effects that have been sent by the effect handler. The loop is automatically disposed when the
 * lifecycle owner is destroyed. To avoid leaks, the maximum number of view effects that are kept
 * when paused is capped - see {@link #create(Function, Object, Init, int)}. Exceeding the limit
 * leads to an {@code IllegalStateException} when posting further effects.
 *
 * <p>This class is {@code public} with a {@code protected} constructor in order to facilitate using
 * it as a key in a {@link android.arch.lifecycle.ViewModelProvider}. It's not intended to be
 * subclassed in order to change its behaviour, and for that reason, all its methods are private or
 * final.
 *
 * @param <M> The Model with which the Mobius Loop will run
 * @param <E> The Event type accepted by the loop
 * @param <F> The Effect type handled by the loop
 * @param <V> The View Effect which will be emitted by this view model
 */
public class MobiusLoopViewModel<M, E, F, V> extends ViewModel {
  private final MutableLiveData<M> modelData = new MutableLiveData<>();
  private final MutableLiveQueue<V> viewEffectQueue;
  private final MobiusLoop<M, E, F> loop;
  private final M startModel;

  protected MobiusLoopViewModel(
      @Nonnull Function<Consumer<V>, Factory<M, E, F>> loopFactoryProvider,
      @Nonnull M modelToStartFrom,
      @Nonnull Init<M, F> init,
      @Nonnull WorkRunner mainLoopWorkRunner,
      int maxEffectQueueSize) {
    final Factory<M, E, F> loopFactory = loopFactoryProvider.apply(this::acceptViewEffect);
    final First<M, F> first = init.init(modelToStartFrom);
    loop = loopFactory.startFrom(first.model(), first.effects());
    startModel = first.model();
    viewEffectQueue = new MutableLiveQueue<>(mainLoopWorkRunner, maxEffectQueueSize);
    loop.observe(this::onModelChanged);
  }

  /**
   * Creates a new MobiusLoopViewModel instance with a default maximum effect queue size.
   *
   * @param loopFactoryProvider provides a way to connect the view's view effect consumer to a loop
   *     factory
   * @param modelToStartFrom the initial model for the loop
   * @param init the {@link Init} function of the loop
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   * @param <V> the view effect type
   */
  public static <M, E, F, V> MobiusLoopViewModel create(
      @Nonnull Function<Consumer<V>, Factory<M, E, F>> loopFactoryProvider,
      @Nonnull M modelToStartFrom,
      @Nonnull Init<M, F> init) {
    return create(loopFactoryProvider, modelToStartFrom, init, 100);
  }

  /**
   * Creates a new MobiusLoopViewModel instance.
   *
   * @param loopFactoryProvider provides a way to connect the view's view effect consumer to a loop
   *     factory
   * @param modelToStartFrom the initial model for the loop
   * @param init the {@link Init} function of the loop
   * @param maxEffectsToQueue the maximum number of effects to queue while paused
   * @param <M> the model type
   * @param <E> the event type
   * @param <F> the effect type
   * @param <V> the view effect type
   */
  public static <M, E, F, V> MobiusLoopViewModel create(
      @Nonnull Function<Consumer<V>, Factory<M, E, F>> loopFactoryProvider,
      @Nonnull M modelToStartFrom,
      @Nonnull Init<M, F> init,
      int maxEffectsToQueue) {
    return new MobiusLoopViewModel<>(
        loopFactoryProvider,
        modelToStartFrom,
        init,
        MainThreadWorkRunner.create(),
        maxEffectsToQueue);
  }

  @Nonnull
  public final M getModel() {
    M model = loop.getMostRecentModel();
    return model != null ? model : startModel;
  }

  @Nonnull
  public final LiveData<M> getModels() {
    return modelData;
  }

  @Nonnull
  public final LiveQueue<V> getViewEffects() {
    return viewEffectQueue;
  }

  public final void dispatchEvent(@Nonnull E event) {
    loop.dispatchEvent(event);
  }

  @Override
  protected final void onCleared() {
    super.onCleared();
    loop.dispose();
  }

  private void onModelChanged(M model) {
    modelData.postValue(model);
  }

  private void acceptViewEffect(V viewEffect) {
    viewEffectQueue.post(viewEffect);
  }
}
