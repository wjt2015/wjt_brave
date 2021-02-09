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
package brave.jms;

import brave.Span;
import brave.Span.Kind;
import brave.propagation.B3SingleFormat;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.Propagation;
import brave.propagation.SamplingFlags;
import brave.propagation.TraceContext;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XAQueueConnection;
import javax.jms.XATopicConnection;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;

import static brave.jms.MessageProperties.setStringProperty;
import static java.util.Arrays.asList;
import static org.apache.activemq.command.ActiveMQDestination.QUEUE_TYPE;
import static org.apache.activemq.command.ActiveMQDestination.createDestination;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

public class JmsTracingTest extends ITJms {
  TraceContext parent = newTraceContext(SamplingFlags.DEBUG);
  ActiveMQTextMessage message = new ActiveMQTextMessage();

  @Test public void connectionFactory_wrapsInput() {
    assertThat(jmsTracing.connectionFactory(mock(ConnectionFactory.class)))
      .isInstanceOf(TracingConnectionFactory.class);
  }

  @Test public void connectionFactory_doesntDoubleWrap() {
    ConnectionFactory wrapped = jmsTracing.connectionFactory(mock(ConnectionFactory.class));
    assertThat(jmsTracing.connectionFactory(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void connectionFactory_wrapsXaInput() {
    abstract class Both implements XAConnectionFactory, ConnectionFactory {
    }

    assertThat(jmsTracing.connectionFactory(mock(Both.class)))
      .isInstanceOf(XAConnectionFactory.class);
  }

  @Test public void connection_wrapsInput() {
    assertThat(jmsTracing.connection(mock(Connection.class)))
      .isInstanceOf(TracingConnection.class);
  }

  @Test public void connection_doesntDoubleWrap() {
    Connection wrapped = jmsTracing.connection(mock(Connection.class));
    assertThat(jmsTracing.connection(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void connection_wrapsXaInput() {
    abstract class Both implements XAConnection, Connection {
    }

    assertThat(jmsTracing.connection(mock(Both.class)))
      .isInstanceOf(XAConnection.class);
  }

  @Test public void queueConnection_wrapsInput() {
    assertThat(jmsTracing.queueConnection(mock(QueueConnection.class)))
      .isInstanceOf(TracingConnection.class);
  }

  @Test public void queueConnection_doesntDoubleWrap() {
    QueueConnection wrapped = jmsTracing.queueConnection(mock(QueueConnection.class));
    assertThat(jmsTracing.queueConnection(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void queueConnection_wrapsXaInput() {
    abstract class Both implements XAQueueConnection, QueueConnection {
    }

    assertThat(jmsTracing.queueConnection(mock(Both.class)))
      .isInstanceOf(XAQueueConnection.class);
  }

  @Test public void topicConnection_wrapsInput() {
    assertThat(jmsTracing.topicConnection(mock(TopicConnection.class)))
      .isInstanceOf(TracingConnection.class);
  }

  @Test public void topicConnection_doesntDoubleWrap() {
    TopicConnection wrapped = jmsTracing.topicConnection(mock(TopicConnection.class));
    assertThat(jmsTracing.topicConnection(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void topicConnection_wrapsXaInput() {
    abstract class Both implements XATopicConnection, TopicConnection {
    }

    assertThat(jmsTracing.topicConnection(mock(Both.class)))
      .isInstanceOf(XATopicConnection.class);
  }

  @Test public void xaConnectionFactory_wrapsInput() {
    assertThat(jmsTracing.xaConnectionFactory(mock(XAConnectionFactory.class)))
      .isInstanceOf(TracingXAConnectionFactory.class);
  }

  @Test public void xaConnectionFactory_doesntDoubleWrap() {
    XAConnectionFactory wrapped = jmsTracing.xaConnectionFactory(mock(XAConnectionFactory.class));
    assertThat(jmsTracing.xaConnectionFactory(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void xaConnection_wrapsInput() {
    assertThat(jmsTracing.xaConnection(mock(XAConnection.class)))
      .isInstanceOf(TracingXAConnection.class);
  }

  @Test public void xaConnection_doesntDoubleWrap() {
    XAConnection wrapped = jmsTracing.xaConnection(mock(XAConnection.class));
    assertThat(jmsTracing.xaConnection(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void xaQueueConnection_wrapsInput() {
    assertThat(jmsTracing.xaQueueConnection(mock(XAQueueConnection.class)))
      .isInstanceOf(TracingXAConnection.class);
  }

  @Test public void xaQueueConnection_doesntDoubleWrap() {
    XAQueueConnection wrapped = jmsTracing.xaQueueConnection(mock(XAQueueConnection.class));
    assertThat(jmsTracing.xaQueueConnection(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void xaTopicConnection_wrapsInput() {
    assertThat(jmsTracing.xaTopicConnection(mock(XATopicConnection.class)))
      .isInstanceOf(TracingXAConnection.class);
  }

  @Test public void xaTopicConnection_doesntDoubleWrap() {
    XATopicConnection wrapped = jmsTracing.xaTopicConnection(mock(XATopicConnection.class));
    assertThat(jmsTracing.xaTopicConnection(wrapped))
      .isSameAs(wrapped);
  }

  @Test public void messageListener_traces() {
    jmsTracing.messageListener(mock(MessageListener.class), false)
      .onMessage(message);

    assertThat(testSpanHandler.takeLocalSpan().name()).isEqualTo("on-message");
  }

  @Test public void messageListener_traces_addsConsumerSpan() {
    jmsTracing.messageListener(mock(MessageListener.class), true)
      .onMessage(message);

    assertThat(asList(testSpanHandler.takeRemoteSpan(Kind.CONSUMER), testSpanHandler.takeLocalSpan()))
      .extracting(brave.handler.MutableSpan::name)
      .containsExactly("receive", "on-message");
  }

  @Test public void messageListener_wrapsInput() {
    assertThat(jmsTracing.messageListener(mock(MessageListener.class), false))
      .isInstanceOf(TracingMessageListener.class);
  }

  @Test public void messageListener_doesntDoubleWrap() {
    MessageListener wrapped = jmsTracing.messageListener(mock(MessageListener.class), false);
    assertThat(jmsTracing.messageListener(wrapped, false))
      .isSameAs(wrapped);
  }

  @Test public void nextSpan_prefers_b3_header() {
    TraceContext incoming = newTraceContext(SamplingFlags.NOT_SAMPLED);
    setStringProperty(message, "b3", B3SingleFormat.writeB3SingleFormat(incoming));

    Span child;
    try (Scope ws = tracing.currentTraceContext().newScope(parent)) {
      child = jmsTracing.nextSpan(message);
    }
    assertChildOf(child.context(), incoming);
    assertThat(child.isNoop()).isTrue();
  }

  @Test public void nextSpan_uses_current_context() {
    Span child;
    try (Scope ws = tracing.currentTraceContext().newScope(parent)) {
      child = jmsTracing.nextSpan(message);
    }
    assertChildOf(child.context(), parent);
  }

  @Test public void nextSpan_should_use_span_from_headers_as_parent() {
    setStringProperty(message, "b3", "0000000000000001-0000000000000002-1");
    Span span = jmsTracing.nextSpan(message);

    assertThat(span.context().parentId()).isEqualTo(2L);
  }

  @Test public void nextSpan_should_create_span_if_no_headers() {
    Span span = jmsTracing.nextSpan(message);

    assertThat(span).isNotNull();
  }

  @Test public void nextSpan_should_tag_queue_when_no_incoming_context() {
    message.setDestination(createDestination("foo", QUEUE_TYPE));
    jmsTracing.nextSpan(message).start().finish();

    assertThat(testSpanHandler.takeLocalSpan().tags())
      .containsOnly(entry("jms.queue", "foo"));
  }

  /**
   * We assume the queue is already tagged by the producer span. However, we can change this policy
   * now, or later when dynamic policy is added to JmsTracing
   */
  @Test public void nextSpan_shouldnt_tag_queue_when_incoming_context() {
    setStringProperty(message, "b3", "0000000000000001-0000000000000002-1");
    message.setDestination(createDestination("foo", QUEUE_TYPE));
    jmsTracing.nextSpan(message).start().finish();

    assertThat(testSpanHandler.takeLocalSpan().tags()).isEmpty();
  }

  @Test public void nextSpan_should_clear_propagation_headers() {
    Propagation.B3_STRING.injector(SETTER).inject(parent, message);
    Propagation.B3_SINGLE_STRING.injector(SETTER).inject(parent, message);

    jmsTracing.nextSpan(message);
    assertThat(ITJms.propertiesToMap(message)).isEmpty();
  }

  @Test public void nextSpan_should_retain_baggage_headers() throws JMSException {
    message.setStringProperty(BAGGAGE_FIELD_KEY, "");

    jmsTracing.nextSpan(message);

    assertThat(message.getStringProperty(BAGGAGE_FIELD_KEY)).isEmpty();
  }

  @Test public void nextSpan_should_not_clear_other_headers() throws JMSException {
    message.setIntProperty("foo", 1);

    jmsTracing.nextSpan(message);

    assertThat(message.getIntProperty("foo")).isEqualTo(1);
  }
}
