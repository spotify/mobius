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
import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.functions.Function;
import javax.annotation.Nonnull;

/**
 * A Mobius Loop controller which is based on the Android ViewModel. <br>
 *
 * <p>This controller has the concept of a View Effect (parameter V) which is a type of effect that
 * requires the corresponding Android lifecycle owner to be in an active state i.e. between onResume
 * and onPause. To allow the normal effect handler to send these, the controller will provide a
 * Consumer of these View Effects to the Loop Factory Provider, which can then be passed into the
 * normal Effect handler so it can delegate view effects where necessary<br>
 *
 * <p>Since it's based on Android View model, this controller will keep the loop alive as long as
 * the lifecycle owner it is associated with (via a factory to produce it) is not destroyed -
 * meaning the Mobius loop will persist through rotations and brief app minimization to background,
 * <br>
 *
 * <p>While the loop is running but the view is paused, which is between onPause and onDestroy, the
 * controller will keep the latest model/state sent by the loop and will keep a queue of View
 * Effects that have been sent by the effect handler. The loop is automatically disposed when the
 * lifecycle owner is destroyed
 *
 * @param <M> The Model with which the Mobius Loop will run
 * @param <E> The Event type accepted by the loop
 * @param <F> The Effect type handled by the loop
 * @param <V> The View Effect which will be emitted by this controller
 */
public class MobiusLoopViewModel<M, E, F, V> extends ViewModel {
  private final MutableLiveData<M> modelData = new MutableLiveData<>();
  private final MutableQueueingSingleLiveData<V> viewEffectData =
      new MutableQueueingSingleLiveData<>();
  private final MobiusLoop<M, E, F> loop;
  private final M startModel;

  public MobiusLoopViewModel(
      @Nonnull Function<Consumer<V>, Factory<M, E, F>> loopFactoryProvider,
      @Nonnull M modelToStartFrom,
      @Nonnull Init<M, F> init) {
    final Factory<M, E, F> loopFactory = loopFactoryProvider.apply(this::acceptViewEffect);
    final First<M, F> first = init.init(modelToStartFrom);
    this.loop = loopFactory.startFrom(first.model(), first.effects());
    this.startModel = first.model();
    loop.observe(this::onModelChanged);
  }

  @Nonnull
  public final M getModel() {
    M model = loop.getMostRecentModel();
    return model != null ? model : startModel;
  }

  @Nonnull
  public final LiveData<M> stateEmitter() {
    return modelData;
  }

  public final SingleLiveData<V> viewEffectEmitter() {
    return viewEffectData;
  }

  public void dispatchEvent(@Nonnull E event) {
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
    viewEffectData.post(viewEffect);
  }
}
