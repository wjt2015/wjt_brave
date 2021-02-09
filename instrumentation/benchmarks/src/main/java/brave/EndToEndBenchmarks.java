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

import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig.SingleBaggageField;
import brave.context.log4j2.ThreadContextScopeDecorator;
import brave.handler.SpanHandler;
import brave.http.HttpServerBenchmarks;
import brave.okhttp3.TracingCallFactory;
import brave.propagation.B3Propagation;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import brave.servlet.TracingFilter;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import java.io.IOException;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import static javax.servlet.DispatcherType.REQUEST;

/** Uses the canonical zipkin frontend-backend app, with the fastest components */
public class EndToEndBenchmarks extends HttpServerBenchmarks {
  public static final BaggageField REQUEST_ID = BaggageField.create("x-vcap-request-id");
  public static final BaggageField COUNTRY_CODE = BaggageField.create("country-code");
  public static final BaggageField USER_ID = BaggageField.create("user-id");
  static volatile int PORT;

  static class HelloServlet extends HttpServlet {
    final Call.Factory callFactory = new OkHttpClient();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
      resp.addHeader("Content-Type", "text/plain; charset=UTF-8");
      // regardless of how we got here, if we are "api" we just return a string
      if (req.getRequestURI().endsWith("/api")) {
        resp.getWriter().println(new Date().toString());
      } else {
        // noop if baggage propagation is not configured
        COUNTRY_CODE.updateValue("FO");
        Request request = new Request.Builder().url(new HttpUrl.Builder()
          .scheme("http")
          .host("127.0.0.1")
          .port(PORT)
          .encodedPath(req.getRequestURI() + "/api").build()).build();

        // If we are tracing, we'll have a scoped call factory available
        Call.Factory localCallFactory =
          (Call.Factory) req.getAttribute(Call.Factory.class.getName());
        if (localCallFactory == null) localCallFactory = callFactory;

        resp.getWriter().println(localCallFactory.newCall(request).execute().body().string());
      }
    }
  }

  public static class Unsampled extends ForwardingTracingFilter {
    public Unsampled() {
      super(Tracing.newBuilder()
        .sampler(Sampler.NEVER_SAMPLE)
        .addSpanHandler(new SpanHandler() {
          // intentionally not NOOP to ensure spans report
        })
        .build());
    }
  }

  public static class OnlySampledLocal extends ForwardingTracingFilter {
    public OnlySampledLocal() {
      super(Tracing.newBuilder()
        .addSpanHandler(new SpanHandler() {
          // anonymous subtype prevents all recording from being no-op
        })
        .alwaysSampleLocal()
        .sampler(Sampler.NEVER_SAMPLE)
        .build());
    }
  }

  public static class Traced extends ForwardingTracingFilter {
    public Traced() {
      super(Tracing.newBuilder()
          .addSpanHandler(new SpanHandler() {
            // intentionally not NOOP to ensure spans report
          })
          .build());
    }
  }

  public static class TracedCorrelated extends ForwardingTracingFilter {
    public TracedCorrelated() {
      super(Tracing.newBuilder()
        .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
          // intentionally added twice to test overhead of multiple correlations
          .addScopeDecorator(ThreadContextScopeDecorator.get())
          .addScopeDecorator(ThreadContextScopeDecorator.get())
          .build())
        .addSpanHandler(new SpanHandler() {
          // intentionally not NOOP to ensure spans report
        })
        .build());
    }
  }

  public static class TracedBaggage extends ForwardingTracingFilter {
    public TracedBaggage() {
      super(Tracing.newBuilder()
        .propagationFactory(BaggagePropagation.newFactoryBuilder(B3Propagation.FACTORY)
          .add(SingleBaggageField.remote(REQUEST_ID))
          .add(SingleBaggageField.newBuilder(COUNTRY_CODE)
            .addKeyName("baggage-country-code")
            .build())
          .add(SingleBaggageField.newBuilder(USER_ID)
            .addKeyName("baggage-user-id")
            .build())
          .build())
        .addSpanHandler(new SpanHandler() {
          // intentionally not NOOP to ensure spans report
        })
        .build());
    }
  }

  public static class Traced128 extends ForwardingTracingFilter {
    public Traced128() {
      super(Tracing.newBuilder()
        .traceId128Bit(true)
        .addSpanHandler(new SpanHandler() {
          // intentionally not NOOP to ensure spans report
        })
        .build());
    }
  }

  @Override protected void init(DeploymentInfo servletBuilder) {
    servletBuilder.addFilter(new FilterInfo("Unsampled", Unsampled.class))
      .addFilterUrlMapping("Unsampled", "/unsampled", REQUEST)
      .addFilterUrlMapping("Unsampled", "/unsampled/api", REQUEST)
      .addFilter(new FilterInfo("OnlySampledLocal", OnlySampledLocal.class))
      .addFilterUrlMapping("OnlySampledLocal", "/onlysampledlocal", REQUEST)
      .addFilterUrlMapping("OnlySampledLocal", "/onlysampledlocal/api", REQUEST)
      .addFilter(new FilterInfo("Traced", Traced.class))
      .addFilterUrlMapping("Traced", "/traced", REQUEST)
      .addFilterUrlMapping("Traced", "/traced/api", REQUEST)
      .addFilter(new FilterInfo("TracedBaggage", TracedBaggage.class))
      .addFilterUrlMapping("TracedBaggage", "/tracedBaggage", REQUEST)
      .addFilterUrlMapping("TracedBaggage", "/tracedBaggage/api", REQUEST)
      .addFilter(new FilterInfo("TracedCorrelated", TracedCorrelated.class))
      .addFilterUrlMapping("TracedCorrelated", "/tracedcorrelated", REQUEST)
      .addFilterUrlMapping("TracedCorrelated", "/tracedcorrelated/api", REQUEST)
      .addFilter(new FilterInfo("Traced128", Traced128.class))
      .addFilterUrlMapping("Traced128", "/traced128", REQUEST)
      .addFilterUrlMapping("Traced128", "/traced128/api", REQUEST)
      .addServlets(Servlets.servlet("HelloServlet", HelloServlet.class).addMapping("/*"));
  }

  @Override protected int initServer() throws Exception {
    return PORT = super.initServer();
  }

  // Convenience main entry-point
  public static void main(String[] args) throws Exception {
    Options opt = new OptionsBuilder()
      .addProfiler("gc")
      .include(".*"
        + EndToEndBenchmarks.class.getSimpleName()
        + ".*(tracedCorrelatedServer_get|tracedServer_get)$")
      .build();

    new Runner(opt).run();
  }

  static class ForwardingTracingFilter implements Filter {
    final Filter delegate;
    final Call.Factory callFactory;

    public ForwardingTracingFilter(Tracing tracing) {
      this.delegate = TracingFilter.create(tracing);
      this.callFactory = TracingCallFactory.create(tracing, new OkHttpClient());
    }

    @Override public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
      servletRequest.setAttribute(Call.Factory.class.getName(), callFactory);
      delegate.doFilter(servletRequest, servletResponse, filterChain);
    }

    @Override public void destroy() {
    }
  }
}
