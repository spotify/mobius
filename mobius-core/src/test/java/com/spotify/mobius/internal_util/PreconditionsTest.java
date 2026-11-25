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
package com.spotify.mobius.internal_util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PreconditionsTest {

  @Test
  public void checkNotNullReturnsValueWhenNonNull() {
    String value = "hello";
    assertThat(Preconditions.checkNotNull(value), is(equalTo(value)));
  }

  @Test(expected = NullPointerException.class)
  public void checkNotNullThrowsNpeWhenNull() {
    Preconditions.checkNotNull(null);
  }

  @Test
  public void checkArrayNoNullsReturnsArrayWhenNoNulls() {
    String[] array = new String[] {"a", "b", "c"};
    assertThat(Preconditions.checkArrayNoNulls(array), is(equalTo(array)));
  }

  @Test(expected = NullPointerException.class)
  public void checkArrayNoNullsThrowsNpeWhenArrayIsNull() {
    Preconditions.checkArrayNoNulls((Object[]) null);
  }

  @Test(expected = NullPointerException.class)
  public void checkArrayNoNullsThrowsNpeWhenElementIsNull() {
    Preconditions.checkArrayNoNulls(new String[] {"a", null, "c"});
  }

  @Test
  public void checkIterableNoNullsReturnsIterableWhenNoNulls() {
    List<String> list = Arrays.asList("a", "b", "c");
    assertThat(Preconditions.checkIterableNoNulls(list), is(equalTo(list)));
  }

  @Test(expected = NullPointerException.class)
  public void checkIterableNoNullsThrowsNpeWhenIterableIsNull() {
    Preconditions.checkIterableNoNulls(null);
  }

  @Test(expected = NullPointerException.class)
  public void checkIterableNoNullsThrowsNpeWhenElementIsNull() {
    Preconditions.checkIterableNoNulls(Arrays.asList("a", null, "c"));
  }
}
