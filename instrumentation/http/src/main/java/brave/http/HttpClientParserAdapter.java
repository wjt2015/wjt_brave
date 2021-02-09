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

import brave.ErrorParser;
import brave.SpanCustomizer;
import brave.propagation.CurrentTraceContext;

/** Added to allow us to keep compatibility with deprecated {@link HttpTracing#clientParser()} */
@Deprecated final class HttpClientParserAdapter extends HttpClientParser {
  final HttpRequestParser requestParser;
  final HttpResponseParser responseParser;
  final CurrentTraceContext currentTraceContext;
  final ErrorParser errorParser;

  HttpClientParserAdapter(
    HttpRequestParser requestParser,
    HttpResponseParser responseParser,
    CurrentTraceContext currentTraceContext,
    ErrorParser errorParser
  ) {
    this.requestParser = requestParser;
    this.responseParser = responseParser;
    this.currentTraceContext = currentTraceContext;
    this.errorParser = errorParser;
  }

  @Override protected ErrorParser errorParser() {
    return errorParser;
  }

  @Override
  public <Req> void request(HttpAdapter<Req, ?> adapter, Req req, SpanCustomizer customizer) {
    HttpRequest request;
    if (req instanceof HttpClientRequest) {
      request = new HttpClientAdapters.FromRequestAdapter<>((HttpClientAdapter) adapter, req);
    } else if (adapter instanceof HttpClientAdapters.ToRequestAdapter) {
      request = ((HttpClientAdapters.ToRequestAdapter) adapter).delegate;
    } else {
      throw new AssertionError("programming bug");
    }
    requestParser.parse(request, currentTraceContext.get(), customizer);
  }

  @Override public <Resp> void response(HttpAdapter<?, Resp> adapter, Resp res, Throwable error,
    SpanCustomizer customizer) {
    HttpResponse response;
    if (res instanceof HttpClientResponse) {
      response =
        new HttpClientAdapters.FromResponseAdapter<>((HttpClientAdapter) adapter, res, error);
    } else if (adapter instanceof HttpClientAdapters.ToResponseAdapter) {
      response = ((HttpClientAdapters.ToResponseAdapter) adapter).delegate;
    } else {
      throw new AssertionError("programming bug");
    }
    responseParser.parse(response, currentTraceContext.get(), customizer);
  }
}
