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
package brave.propagation;

import brave.propagation.CurrentTraceContext.Scope;
import brave.test.propagation.CurrentTraceContextTest;
import java.util.function.Supplier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StrictCurrentTraceContextTest extends CurrentTraceContextTest {
  @Override protected Class<? extends Supplier<CurrentTraceContext.Builder>> builderSupplier() {
    return BuilderSupplier.class;
  }

  static class BuilderSupplier implements Supplier<CurrentTraceContext.Builder> {
    @Override public CurrentTraceContext.Builder get() {
      return new StrictCurrentTraceContext.Builder();
    }
  }

  @Test public void scope_enforcesCloseOnSameThread() throws InterruptedException {
    final Exception[] spawnedThreadException = new Exception[1];
    Thread scopingThread = new Thread(() -> {
      try (Scope scope = currentTraceContext.newScope(context)) {
        Thread spawnedThread = new Thread(() -> { // should not inherit scope!
          try {
            scope.close();
          } catch (IllegalStateException e) {
            spawnedThreadException[0] = e;
          }
        }, "spawned thread");
        spawnedThread.start();
        spawnedThread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
        Thread.currentThread().interrupt();
      }
    }, "scoping thread");

    scopingThread.start();
    scopingThread.join();

    assertThat(spawnedThreadException[0])
      .hasMessage("Thread [scoping thread] opened scope, but thread [spawned thread] closed it")
      .hasRootCauseMessage("Thread [scoping thread] opened scope for " + context + " here:");
  }
}
