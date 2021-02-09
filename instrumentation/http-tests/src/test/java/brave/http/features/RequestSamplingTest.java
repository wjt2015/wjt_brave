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
package brave.http.features;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.propagation.StrictCurrentTraceContext;
import brave.test.IntegrationTestSpanHandler;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static brave.Span.Kind.CLIENT;
import static brave.Span.Kind.SERVER;
import static brave.sampler.SamplerFunctions.neverSample;
import static org.assertj.core.api.Assertions.assertThat;

/** This is an example of http request sampling */
public class RequestSamplingTest {
  @Rule public MockWebServer server = new MockWebServer();
  @Rule public IntegrationTestSpanHandler testSpanHandler = new IntegrationTestSpanHandler();

  StrictCurrentTraceContext currentTraceContext = StrictCurrentTraceContext.create();
  Tracing tracing = Tracing.newBuilder()
    .localServiceName("server")
    .currentTraceContext(currentTraceContext)
    .addSpanHandler(testSpanHandler)
    .build();
  HttpTracing httpTracing = HttpTracing.newBuilder(tracing)
    // server starts traces under the path /api
    .serverSampler((request) -> request.path().startsWith("/api"))
    // client doesn't start new traces
    .clientSampler(neverSample())
    .build();

  OkHttpClient client = new OkHttpClient();

  @Before public void setup() {
    server.setDispatcher(new TracingDispatcher(httpTracing, new Dispatcher() {
      OkHttpClient tracedClient = client.newBuilder()
        .addNetworkInterceptor(new TracingInterceptor(httpTracing)).build();

      @Override public MockResponse dispatch(RecordedRequest request) {
        if (request.getPath().equals("/next")) return new MockResponse().setBody("next");
        Call next = tracedClient.newCall(new Request.Builder().url(server.url("/next")).build());
        try (ResponseBody responseBody = next.execute().body()) {
          return new MockResponse().setBody(responseBody.string());
        } catch (IOException e) {
          return new MockResponse().setBody(e.getMessage()).setResponseCode(500);
        }
      }
    }));
  }

  @After public void close() {
    tracing.close();
    currentTraceContext.close();
  }

  @Test public void serverDoesntTraceFoo() throws Exception {
    callServer("/foo");
  }

  @Test public void clientTracedWhenServerIs() throws Exception {
    callServer("/api");

    assertThat(testSpanHandler.takeRemoteSpan(SERVER).tags())
        .containsEntry("http.path", "/next");
    assertThat(testSpanHandler.takeRemoteSpan(CLIENT).tags())
        .containsEntry("http.path", "/next");
    assertThat(testSpanHandler.takeRemoteSpan(SERVER).tags())
        .containsEntry("http.path", "/api");
  }

  void callServer(String path) throws IOException {
    Call next = client.newCall(new Request.Builder().url(server.url(path)).build());
    try (ResponseBody responseBody = next.execute().body()) {
      assertThat(responseBody.string()).isEqualTo("next");
    }
  }
}
