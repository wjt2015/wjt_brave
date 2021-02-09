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
package brave.http;

import zipkin2.Endpoint;

/** @deprecated Since 5.10, use {@link HttpClientRequest} and {@link HttpClientResponse} */
@Deprecated public abstract class HttpClientAdapter<Req, Resp> extends HttpAdapter<Req, Resp> {
  /**
   * Returns true if an IP representing the client was readable.
   *
   * @deprecated remote IP information should be added directly by instrumentation. This will be
   * removed in Brave v6.
   */
  @Deprecated public boolean parseServerIpAndPort(Req req, Endpoint.Builder builder) {
    return false;
  }
}
