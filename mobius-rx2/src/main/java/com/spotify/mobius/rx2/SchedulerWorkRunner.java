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
package com.spotify.mobius.rx2;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.runners.WorkRunner;
import io.reactivex.Scheduler;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;

public class SchedulerWorkRunner implements WorkRunner {

  private final Scheduler.Worker worker;
  @Nonnull private final Lock lock = new ReentrantLock();

  public SchedulerWorkRunner(Scheduler scheduler) {
    this.worker = checkNotNull(scheduler).createWorker();
  }

  @Override
  public void post(final Runnable runnable) {
    lock.lock();
    try {
      worker.schedule(runnable);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void dispose() {
    lock.lock();
    try {
      worker.dispose();
    } finally {
      lock.unlock();
    }
  }
}
