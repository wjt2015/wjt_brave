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
package brave.netty.http;

import brave.http.HttpServerRequest;
import brave.netty.http.TracingHttpServerHandler.HttpResponseWrapper;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpResponseWrapperTest {
  @Mock HttpServerRequest request;
  @Mock HttpResponse response;
  @Mock HttpResponseStatus status;

  @Test public void request() {
    assertThat(new HttpResponseWrapper(request, response, null).request())
      .isSameAs(request);
  }

  @Test public void statusCode() {
    when(response.status()).thenReturn(status);
    when(status.code()).thenReturn(200);

    assertThat(new HttpResponseWrapper(request, response, null).statusCode()).isEqualTo(200);
  }

  @Test public void statusCode_zeroNoResponse() {
    assertThat(new HttpResponseWrapper(request, response, null).statusCode()).isZero();
  }
}
