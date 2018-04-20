package com.spotify.mobius;

import com.spotify.mobius.functions.Consumer;
import java.util.Collections;
import java.util.List;
import org.assertj.core.util.Lists;

class TestConsumer<T> implements Consumer<T> {

  final List<T> received = Collections.synchronizedList(Lists.<T>newArrayList());

  @Override
  public void accept(T value) {
    received.add(value);
  }
}
