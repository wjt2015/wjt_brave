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

import brave.Clock;
import brave.Request;
import brave.internal.Nullable;
import brave.propagation.TraceContext;

/**
 * Abstract request type used for parsing and sampling of http clients and servers.
 *
 * @see HttpClientRequest
 * @see HttpServerRequest
 * @since 5.8
 */
public abstract class HttpRequest extends Request {
  /**
   * The timestamp in epoch microseconds of the beginning of this request or zero to take this
   * implicitly from the current clock. Defaults to zero.
   *
   * <p>This is helpful in two scenarios: late parsing and avoiding redundant timestamp overhead.
   * If a server span, this helps reach the "original" beginning of the request, which is always
   * prior to parsing.
   *
   * <p>Note: Overriding has the same problems as using {@link brave.Span#start(long)}. For
   * example, it can result in negative duration if the clock used is allowed to correct backwards.
   * It can also result in misalignments in the trace, unless {@link brave.Tracing.Builder#clock(Clock)}
   * uses the same implementation.
   *
   * @see HttpResponse#finishTimestamp()
   * @see brave.Span#start(long)
   * @see brave.Tracing#clock(TraceContext)
   * @since 5.8
   */
  public long startTimestamp() {
    return 0L;
  }

  /**
   * The HTTP method, or verb, such as "GET" or "POST".
   *
   * <p>Conventionally associated with the key "http.method"
   *
   * <h3>Note</h3>
   * <p>It is part of the <a href="https://tools.ietf.org/html/rfc7231#section-4.1">HTTP RFC</a>
   * that an HTTP method is case-sensitive. Do not downcase results. If you do, not only will
   * integration tests fail, but you will surprise any consumers who expect compliant results.
   *
   * @see HttpTags#METHOD
   */
  public abstract String method();

  /**
   * The absolute http path, without any query parameters. Ex. "/objects/abcd-ff"
   *
   * <p>Conventionally associated with the key "http.path"
   *
   * <p>{@code null} could mean not applicable to the HTTP method (ex CONNECT).
   *
   * <h3>Implementation notes</h3>
   * Some HTTP client abstractions, such as JAX-RS and spring-web, return the input as opposed to
   * the absolute path. One common problem is a path requested as "", not "/". When that's the case,
   * normalize "" to "/". This ensures values are consistent with wire-level clients and behaviour
   * consistent with RFC 7230 Section 2.7.3.
   *
   * <p>Ex.
   * <pre>{@code
   * @Override public String path() {
   *   String result = delegate.getURI().getPath();
   *   return result != null && result.isEmpty() ? "/" : result;
   * }
   * }</pre>
   *
   * @see #url()
   * @see HttpResponse#route()
   * @see HttpTags#PATH
   */
  @Nullable public abstract String path();

  /**
   * Returns an expression such as "/items/:itemId" representing an application endpoint,
   * conventionally associated with the tag key "http.route". If no route matched, "" (empty string)
   * is returned. Null indicates this instrumentation doesn't understand http routes.
   *
   * <p>The route is associated with the request, but it may not be visible until response
   * processing. The reasons is that many server implementations process the request before they can
   * identify the route. Parsing should expect this and look at {@link HttpResponse#route()} as
   * needed.
   *
   * @see HttpRequest#path()
   * @see HttpTags#ROUTE
   * @since 5.10
   */
  @Nullable public String route() {
    return null;
  }

  /**
   * The entire URL, including the scheme, host and query parameters if available or null if
   * unreadable.
   *
   * <p>Conventionally associated with the key "http.url"
   *
   * @see #path()
   * @see HttpResponse#route()
   * @see HttpTags#URL
   */
  @Nullable public abstract String url();

  /** Returns one value corresponding to the specified header, or null. */
  @Nullable public abstract String header(String name);

  HttpRequest() { // sealed type: only client and server
  }
}
