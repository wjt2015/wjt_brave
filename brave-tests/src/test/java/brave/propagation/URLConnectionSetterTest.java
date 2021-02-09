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

import brave.propagation.Propagation.Setter;
import brave.test.propagation.PropagationSetterTest;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;

/** Example setter test */
public class URLConnectionSetterTest extends PropagationSetterTest<URLConnection> {
  final URLConnection request;

  public URLConnectionSetterTest() throws IOException {
    request = new URLConnection(URI.create("http://127.0.0.1:9999").toURL()) {
      @Override public void connect() {
      }
    };
  }

  @Override protected URLConnection request() {
    return request;
  }

  @Override protected Setter<URLConnection, String> setter() {
    return URLConnection::setRequestProperty;
  }

  @Override protected Iterable<String> read(URLConnection request, String key) {
    return request.getRequestProperties().get(key);
  }
}
