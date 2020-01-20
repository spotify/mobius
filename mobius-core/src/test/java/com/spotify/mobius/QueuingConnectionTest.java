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
package com.spotify.mobius;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.spotify.mobius.test.RecordingConnection;
import org.junit.Before;
import org.junit.Test;

public class QueuingConnectionTest {

  private QueuingConnection<String> connection;
  private RecordingConnection<String> delegate;

  @Before
  public void setUp() throws Exception {
    delegate = new RecordingConnection<>();

    connection = new QueuingConnection<>();
  }

  @Test
  public void shouldForwardAcceptToDelegateWhenAvailable() throws Exception {
    connection.setDelegate(delegate);

    connection.accept("hey there");
    connection.accept("hi!");

    delegate.assertValues("hey there", "hi!");
  }

  @Test
  public void shouldQueueAcceptBeforeDelegateAvailable() throws Exception {

    connection.accept("hey there");
    connection.accept("hi!");

    // nothing yet
    delegate.assertValues();

    // provide delegate and expect the values to show up
    connection.setDelegate(delegate);

    delegate.assertValues("hey there", "hi!");
  }

  @Test
  public void shouldForwardDisposeToDelegate() throws Exception {
    connection.setDelegate(delegate);
    connection.dispose();

    assertThat(delegate.disposed, is(true));
  }

  @Test
  public void shouldSupportDisposeWithoutDelegate() throws Exception {
    connection.dispose();
  }

  @Test
  public void shouldNotForwardQueuedValuesAfterDispose() throws Exception {
    connection.accept("don't want to see this");

    connection.dispose();

    connection.setDelegate(delegate);

    delegate.assertValues();
  }

  @Test
  public void shouldNotAllowDuplicateDelegates() throws Exception {
    connection.setDelegate(delegate);

    assertThatThrownBy(() -> connection.setDelegate(new RecordingConnection<>()))
        .isInstanceOf(IllegalStateException.class);
  }
}
