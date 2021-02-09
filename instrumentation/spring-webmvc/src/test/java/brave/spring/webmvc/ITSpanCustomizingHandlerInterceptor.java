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
package brave.spring.webmvc;

import brave.test.http.Jetty9ServerController;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class ITSpanCustomizingHandlerInterceptor extends BaseITSpanCustomizingHandlerInterceptor {
  public ITSpanCustomizingHandlerInterceptor() {
    super(new Jetty9ServerController());
  }

  @Override protected void addDelegatingTracingFilter(ServletContextHandler handler) {
    handler.addFilter(DelegatingTracingFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
  }

  @Override
  protected void registerTestController(AnnotationConfigWebApplicationContext appContext) {
    appContext.register(Servlet3TestController.class); // the test resource
  }
}
