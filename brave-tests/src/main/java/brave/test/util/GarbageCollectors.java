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

package brave.test.util;

import java.lang.ref.WeakReference;

/**
 * Utilities for working with the garbage collector in tests.
 */
public final class GarbageCollectors {

  /**
   * Runs the garbage collector and waits until all of the provided {@link WeakReference} are
   * cleared, indicating the referenced objects have been collected.
   */
  public static void blockOnGC() {
    System.gc();
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }

  private GarbageCollectors() {}
}
