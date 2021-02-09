/*
 * Copyright 2013-2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package brave.messaging;

import brave.Tracing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// copy of tests in CurrentTracingTest as same pattern is used
public class CurrentMessagingTracingTest {
  Tracing tracing = mock(Tracing.class);

  @Before public void reset() {
    MessagingTracing.CURRENT.set(null);
  }

  @After public void close() {
    MessagingTracing current = MessagingTracing.current();
    if (current != null) current.close();
  }

  @Test public void defaultsToNull() {
    assertThat(MessagingTracing.current()).isNull();
  }

  @Test public void autoRegisters() {
    MessagingTracing current = MessagingTracing.create(tracing);

    assertThat(MessagingTracing.current())
      .isSameAs(current);
  }

  @Test public void setsNotCurrentOnClose() {
    autoRegisters();

    MessagingTracing.current().close();

    assertThat(MessagingTracing.current()).isNull();
  }

  @Test public void canSetCurrentAgain() {
    setsNotCurrentOnClose();

    autoRegisters();
  }

  @Test public void onlyRegistersOnce() throws InterruptedException {
    final MessagingTracing[] threadValues =
      new MessagingTracing[10]; // array ref for thread-safe setting

    List<Thread> getOrSet = new ArrayList<>(20);

    for (int i = 0; i < 10; i++) {
      final int index = i;
      getOrSet.add(new Thread(() -> threadValues[index] = MessagingTracing.current()));
    }
    for (int i = 10; i < 20; i++) {
      getOrSet.add(new Thread(() -> MessagingTracing.create(tracing)));
    }

    // make it less predictable
    Collections.shuffle(getOrSet);

    // start the races
    getOrSet.forEach(Thread::start);
    for (Thread thread : getOrSet) {
      thread.join();
    }

    Set<MessagingTracing> messagingTracings = new LinkedHashSet<>(Arrays.asList(threadValues));
    messagingTracings.remove(null);
    // depending on race, we should have either one instance or none
    assertThat(messagingTracings.isEmpty() || messagingTracings.size() == 1)
      .isTrue();
  }
}
