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
package brave;

/**
 * Performs no operations as the span represented by this is not sampled to report to the tracing
 * system.
 */
// Preferred to a constant NOOP in SpanCustomizer as the latter ends up in a hierarchy with Span
public enum NoopSpanCustomizer implements SpanCustomizer {
  INSTANCE;

  @Override public SpanCustomizer name(String name) {
    return this;
  }

  @Override public SpanCustomizer tag(String key, String value) {
    return this;
  }

  @Override public SpanCustomizer annotate(String value) {
    return this;
  }

  @Override public String toString() {
    return "NoopSpanCustomizer{}";
  }
}
