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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestTest {
  @Test public void toString_mentionsDelegate() {
    class IceCreamRequest extends Request {
      @Override public Span.Kind spanKind() {
        return Span.Kind.SERVER;
      }

      @Override public Object unwrap() {
        return "chocolate";
      }
    }
    assertThat(new IceCreamRequest())
      .hasToString("IceCreamRequest{chocolate}");
  }

  @Test public void toString_doesntStackoverflowWhenUnwrapIsThis() {
    class BuggyRequest extends Request {
      @Override public Object unwrap() {
        return this;
      }

      @Override public Span.Kind spanKind() {
        return Span.Kind.SERVER;
      }
    }
    assertThat(new BuggyRequest())
      .hasToString("BuggyRequest");
  }

  @Test public void toString_doesntNPEWhenUnwrapIsNull() {
    class NoRequest extends Request {
      @Override public Object unwrap() {
        return null;
      }

      @Override public Span.Kind spanKind() {
        return Span.Kind.SERVER;
      }
    }
    assertThat(new NoRequest())
      .hasToString("NoRequest");
  }
}
