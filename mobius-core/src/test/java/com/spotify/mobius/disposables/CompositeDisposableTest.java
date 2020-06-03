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
package com.spotify.mobius.disposables;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class CompositeDisposableTest {

  @Test
  public void shouldDisposeAllIncludedDisposables() throws Exception {
    TestDisposable one = new TestDisposable();
    TestDisposable two = new TestDisposable();
    TestDisposable three = new TestDisposable();

    Disposable composite = CompositeDisposable.from(one, two, three);

    composite.dispose();

    assertThat(one.disposed, is(true));
    assertThat(two.disposed, is(true));
    assertThat(three.disposed, is(true));
  }

  @Test
  public void changingArrayAfterCreatingHasNoEffect() throws Exception {
    TestDisposable one = new TestDisposable();
    TestDisposable two = new TestDisposable();
    TestDisposable three = new TestDisposable();
    TestDisposable four = new TestDisposable();
    TestDisposable five = new TestDisposable();
    TestDisposable six = new TestDisposable();

    Disposable[] disposables = new Disposable[] {one, two, three};

    Disposable composite = CompositeDisposable.from(disposables);

    disposables[0] = four;
    disposables[1] = five;
    disposables[2] = six;

    composite.dispose();

    assertThat(one.disposed, is(true));
    assertThat(two.disposed, is(true));
    assertThat(three.disposed, is(true));
    assertThat(four.disposed, is(false));
    assertThat(five.disposed, is(false));
    assertThat(six.disposed, is(false));
  }

  private static class TestDisposable implements Disposable {
    private boolean disposed = false;

    @Override
    public void dispose() {
      disposed = true;
    }
  }
}
