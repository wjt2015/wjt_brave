/*
 * Copyright 2013-2020 The OpenZipkin Authors
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
import org.junit.Test;

import static brave.test.util.ClassLoaders.assertRunIsUnloadable;
import static org.assertj.core.api.Assertions.assertThat;

public class MessagingTracingClassLoaderTest {
  @Test public void unloadable_afterClose() {
    assertRunIsUnloadable(ClosesMessagingTracing.class, getClass().getClassLoader());
  }

  static class ClosesMessagingTracing implements Runnable {
    @Override public void run() {
      try (Tracing tracing = Tracing.newBuilder().build();
           MessagingTracing messagingTracing = MessagingTracing.create(tracing)) {
      }
    }
  }

  @Test public void unloadable_afterBasicUsage() {
    assertRunIsUnloadable(BasicUsage.class, getClass().getClassLoader());
  }

  static class BasicUsage implements Runnable {
    @Override public void run() {
      try (Tracing tracing = Tracing.newBuilder().build();
           MessagingTracing messagingTracing = MessagingTracing.create(tracing)) {
        messagingTracing.consumerSampler().trySample(null);
      }
    }
  }

  @Test public void unloadable_forgetClose() {
    assertRunIsUnloadable(ForgetClose.class, getClass().getClassLoader());
  }

  static class ForgetClose implements Runnable {
    @Override public void run() {
      try (Tracing tracing = Tracing.newBuilder().build()) {
        MessagingTracing.create(tracing);
        assertThat(MessagingTracing.current()).isNotNull();
      }
    }
  }
}
