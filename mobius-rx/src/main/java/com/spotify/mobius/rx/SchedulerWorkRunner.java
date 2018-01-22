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
package com.spotify.mobius.rx;

import com.spotify.mobius.runners.WorkRunner;
import rx.Scheduler;
import rx.functions.Action0;

/** A {@link WorkRunner} that is backed by an Rx {@link Scheduler} for running work. */
public class SchedulerWorkRunner implements WorkRunner {

  private final Scheduler.Worker worker;

  public SchedulerWorkRunner(Scheduler scheduler) {
    this.worker = scheduler.createWorker();
  }

  @Override
  public void post(final Runnable runnable) {
    worker.schedule(
        new Action0() {
          @Override
          public void call() {
            runnable.run();
          }
        });
  }

  @Override
  public void dispose() {
    worker.unsubscribe();
  }
}
