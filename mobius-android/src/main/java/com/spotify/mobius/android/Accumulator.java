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

import com.spotify.mobius.functions.Consumer;
import java.util.LinkedList;
import java.util.Queue;
import javax.annotation.Nonnull;

/**
 * A mutable object queue container that allows objects to be accessed only once before being
 * cleared
 *
 * @param <T> The object to store
 */
public class Accumulator<T> {
  private final Queue<T> objectQueue;

  Accumulator() {
    objectQueue = new LinkedList<>();
  }

  /**
   * Sends each and every object stored in this container once, and only once and removes each of
   * them from the queue
   *
   * @param objectConsumer The consumer to receive each object
   */
  public void handle(@Nonnull Consumer<T> objectConsumer) {
    synchronized (objectQueue) {
      while (!objectQueue.isEmpty()) objectConsumer.accept(objectQueue.poll());
    }
  }

  /**
   * Appends the given event to this accumulator's queue
   *
   * @param object The object to store
   * @return this accumulator, for convenience
   */
  @Nonnull
  Accumulator<T> append(@Nonnull T object) {
    synchronized (objectQueue) {
      objectQueue.add(object);
    }
    return this;
  }
}
