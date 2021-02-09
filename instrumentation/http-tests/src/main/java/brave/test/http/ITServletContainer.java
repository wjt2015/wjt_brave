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
package brave.test.http;

import brave.test.http.ServletContainer.ServerController;
import javax.servlet.UnavailableException;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.junit.After;

/** Starts a jetty server which runs a servlet container */
public abstract class ITServletContainer extends ITHttpServer {
  public static final UnavailableException NOT_READY_UE =
    new UnavailableException(NOT_READY_ISE.getMessage(), 1 /* temporary implies 503 */) {
      @Override public Throwable fillInStackTrace() {
        return this; // don't fill logs as we are only testing
      }
    };

  final ServerController serverController;

  protected ITServletContainer(ServerController serverController) {
    Log.setLog(new Log4J2Log());
    this.serverController = serverController;
  }

  ServletContainer container;

  protected ServletContainer newServletContainer() {
    return new ServletContainer(serverController) {
      @Override public void init(ServletContextHandler handler) {
        ITServletContainer.this.init(handler);
      }
    };
  }

  /** recreates the server so that it uses the supplied trace configuration */
  @Override protected final void init() {
    container = newServletContainer();
    container.init();
  }

  @Override protected final String url(String path) {
    return container.url(path);
  }

  /** Implement by registering a servlet for the test resource and anything needed for tracing */
  public abstract void init(ServletContextHandler handler);

  @After
  public void stop() {
    container.stop();
  }
}
