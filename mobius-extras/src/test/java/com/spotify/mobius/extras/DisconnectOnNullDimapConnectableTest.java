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
package com.spotify.mobius.extras;

import static com.spotify.mobius.extras.TestConnectable.State.CONNECTED;
import static com.spotify.mobius.extras.TestConnectable.State.DISPOSED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.spotify.mobius.Connectable;
import com.spotify.mobius.Connection;
import com.spotify.mobius.extras.domain.A;
import com.spotify.mobius.extras.domain.B;
import com.spotify.mobius.extras.domain.C;
import com.spotify.mobius.extras.domain.D;
import com.spotify.mobius.test.RecordingConsumer;
import org.junit.Before;
import org.junit.Test;

public class DisconnectOnNullDimapConnectableTest {
  private TestConnectable connectable;
  private Connectable<A, D> underTest;
  private RecordingConsumer<D> recorder;
  private Connection<A> connection;

  @Before
  public void setUp() {
    connectable = TestConnectable.createWithReversingTransformation();
    underTest = Connectables.dimap(A::b, D::create, connectable);
    recorder = new RecordingConsumer<>();
  }

  @Test
  public void doesNothingIfWrappingConnectableIsntConnected() {
    assertNull(connectable.state);
  }

  @Test
  public void connectsToTargetWhenReceivingNonnullValue() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    assertEquals(CONNECTED, connectable.state);
  }

  @Test
  public void connectsToTargetOnlyOnReceivingFirstNonnullValue() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    connection.accept(A.create(B.create("World")));
    assertEquals(CONNECTED, connectable.state);
    assertEquals(1, connectable.connectionsCount);
  }

  @Test
  public void doesNotConnectToTargetOnReceivingNullValues() {
    connect();
    connection.accept(A.create(null));
    assertNull(connectable.state);
    assertEquals(0, connectable.connectionsCount);
  }

  @Test
  public void disconnectsFromTargetAfterReceivingNullValue() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    assertEquals(CONNECTED, connectable.state);
    connection.accept(A.create(null));
    assertEquals(DISPOSED, connectable.state);
  }

  @Test
  public void reconnectsOnValidValuesAfterDisconnectingFromTarget() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    assertEquals(CONNECTED, connectable.state);
    connection.accept(A.create(null));
    assertEquals(DISPOSED, connectable.state);

    connection.accept(A.create(B.create("World")));
    assertEquals(CONNECTED, connectable.state);
  }

  @Test
  public void connectionDisposalPropagatesToWrappedConnectable() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    assertEquals(CONNECTED, connectable.state);
    connection.dispose();
    assertEquals(DISPOSED, connectable.state);
  }

  @Test
  public void connectionDisposalWithoutBeingConnectedToWrappedConnectableDoesNothing() {
    connect();
    connection.dispose();
    assertNull(connectable.state);
  }

  @Test
  public void unpacksAndDelegatesToChildConnectableAndMapsChildOutput() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    recorder.assertValues(D.create(C.create("olleH")));
  }

  @Test
  public void unpacksAndDelegatesToChildConnectableAndMapsChildOutputMultipleTimes() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    connection.accept(A.create(B.create("World")));
    recorder.assertValues(D.create(C.create("olleH")), D.create(C.create("dlroW")));
  }

  @Test
  public void
      unpacksAndDelegatesToChildConnectableAndMapsChildOutputMultipleTimesWithDisconnectingBetweenItems() {
    connect();
    connection.accept(A.create(B.create("Hello")));
    connection.accept(A.create(null));
    connection.accept(A.create(B.create("World")));
    recorder.assertValues(D.create(C.create("olleH")), D.create(C.create("dlroW")));
  }

  private void connect() {
    connection = underTest.connect(recorder);
  }
}
