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

import com.google.auto.value.AutoValue;
import com.spotify.mobius.Connectable;
import com.spotify.mobius.EventSource;
import com.spotify.mobius.Init;
import com.spotify.mobius.Mobius;
import com.spotify.mobius.MobiusLoop;
import com.spotify.mobius.Update;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class defines a Mobius program. It holds you your {@link Init}, {@link Update}, {@link
 * EventSource}, and {@link Connectable} definitions, as well as the tag you would like to use for
 * identifying your program in logs. It is primarily meant to be used for composing programs, in
 * most cases you should prefer to use {@link Mobius#loop(Update, Connectable)}.
 */
@AutoValue
public abstract class Program<M, E, F> {

  @Nonnull
  public abstract Update<M, E, F> update();

  @Nonnull
  public abstract Connectable<F, E> effectHandler();

  @Nullable
  public abstract Init<M, F> init();

  @Nullable
  public abstract EventSource<E> eventSource();

  @Nullable
  public abstract String loggingTag();

  /** @return a {@link MobiusLoop.Builder} based on this program */
  public MobiusLoop.Builder<M, E, F> createLoop() {
    MobiusLoop.Builder<M, E, F> builder = Mobius.loop(update(), effectHandler());

    Init<M, F> init = init();
    if (init != null) {
      builder = builder.init(init);
    }

    EventSource<E> eventSource = eventSource();
    if (eventSource != null) {
      builder = builder.eventSource(eventSource);
    }

    String loggingTag = loggingTag();
    if (loggingTag != null) {
      builder = builder.logger(SLF4JLogger.<M, E, F>withTag(loggingTag));
    }

    return builder;
  }

  public static <M, E, F> Builder<M, E, F> builder() {
    return new AutoValue_Program.Builder<>();
  }

  @AutoValue.Builder
  public abstract static class Builder<M, E, F> {
    public abstract Builder<M, E, F> update(Update<M, E, F> update);

    public abstract Builder<M, E, F> effectHandler(Connectable<F, E> effectHandler);

    public abstract Builder<M, E, F> init(Init<M, F> init);

    public abstract Builder<M, E, F> eventSource(EventSource<E> eventSource);

    public abstract Builder<M, E, F> loggingTag(String loggingTag);

    public abstract Program<M, E, F> build();
  }
}
