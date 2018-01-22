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
package com.spotify.mobius.internal_util;

import static com.spotify.mobius.internal_util.ImmutableUtil.setOf;
import static com.spotify.mobius.internal_util.ImmutableUtil.unionSets;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import org.junit.Test;

public class ImmutableUtilTest {
  @Test
  public void shouldMergeSetsCorrectly() throws Exception {
    assertThat(
        unionSets(Sets.newHashSet("e1", "e2"), setOf("e3", "e4")),
        equalTo(setOf("e1", "e2", "e3", "e4")));
  }
}
