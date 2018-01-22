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
package com.spotify.mobius.runners;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExecutorServiceWorkRunnerTest {

  private ExecutorServiceWorkRunner underTest;

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    underTest = new ExecutorServiceWorkRunner(Executors.newSingleThreadExecutor());
  }

  @Test
  public void shouldNotReturnFromDisposeUntilFinishedRunning() throws Exception {
    final Semaphore blockBackground = new Semaphore(0);
    final Semaphore blockUnderTest = new Semaphore(0);
    final Semaphore blockMainThread = new Semaphore(0);

    final List<Integer> output = new CopyOnWriteArrayList<>();

    underTest.post(
        new Runnable() {
          @Override
          public void run() {
            output.add(1);
            blockBackground.release();
            blockUnderTest.acquireUninterruptibly();
            output.add(3);
            blockMainThread.release();
          }
        });

    ExecutorServiceWorkRunner backgroundWorkRunner =
        new ExecutorServiceWorkRunner(Executors.newSingleThreadExecutor());
    backgroundWorkRunner.post(
        new Runnable() {
          @Override
          public void run() {
            blockBackground.acquireUninterruptibly();
            output.add(2);
            blockUnderTest.release();
          }
        });

    blockMainThread.acquire();
    underTest.dispose();
    output.add(4);

    Thread.sleep(40); // wait a bit and make sure nothing else is added after the 4

    assertThat(output, equalTo(asList(1, 2, 3, 4)));
  }

  @Test
  public void disposingShouldStopUnderlyingExecutorService() throws Exception {
    ExecutorService service = Executors.newSingleThreadExecutor();

    underTest = new ExecutorServiceWorkRunner(service);
    underTest.dispose();

    assertThat(service.isTerminated(), is(true));
  }

  @Test
  public void tasksShouldBeRejectedAfterDispose() throws Exception {
    ExecutorService service = Executors.newSingleThreadExecutor();

    underTest = new ExecutorServiceWorkRunner(service);
    underTest.dispose();

    thrown.expect(RejectedExecutionException.class);

    underTest.post(
        new Runnable() {
          @Override
          public void run() {
            System.err.println("ERROR: this shouldn't run/be printed!");
          }
        });
  }

  @Test
  public void disposeShouldContinueDespiteUnterminatedTask() throws Exception {
    final AtomicBoolean alwaysTrue = new AtomicBoolean(true);

    underTest.post(
        new Runnable() {
          @Override
          public void run() {
            while (alwaysTrue.get()) {
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
          }
        });

    // should terminate with no exceptions, but should log a warning about an unterminated task
    underTest.dispose();
  }

  @Test
  public void disposeShouldContinueDespiteUnterminatedAndQueuedTasks() throws Exception {
    final AtomicBoolean alwaysTrue = new AtomicBoolean(true);

    underTest.post(
        new Runnable() {
          @Override
          public void run() {
            while (alwaysTrue.get()) {
              Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            }
          }
        });
    underTest.post(
        new Runnable() {
          @Override
          public void run() {
            System.err.println("Don't want to see this!");
          }
        });

    // should terminate with no exceptions, but should log a warning about a queued and an
    // unterminated task
    underTest.dispose();
  }
}
