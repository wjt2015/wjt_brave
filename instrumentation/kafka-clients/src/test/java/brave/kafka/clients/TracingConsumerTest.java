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
package brave.kafka.clients;

import brave.handler.MutableSpan;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Test;

import static brave.Span.Kind.CONSUMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TracingConsumerTest extends KafkaTest {
  MockConsumer<String, String> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
  TopicPartition topicPartition = new TopicPartition(TEST_TOPIC, 0);

  @Before
  public void before() {
    Map<TopicPartition, Long> offsets = new HashMap<>();
    offsets.put(topicPartition, 0L);

    consumer.updateBeginningOffsets(offsets);
    consumer.assign(offsets.keySet());
  }

  @Test
  public void should_call_wrapped_poll_and_close_spans() {
    consumer.addRecord(consumerRecord);
    Consumer<String, String> tracingConsumer = kafkaTracing.consumer(consumer);
    tracingConsumer.poll(10);

    // offset changed
    assertThat(consumer.position(topicPartition)).isEqualTo(2L);


    MutableSpan consumerSpan = spans.get(0);
    assertThat(consumerSpan.kind()).isEqualTo(CONSUMER);
    assertThat(consumerSpan.name()).isEqualTo("poll");
    assertThat(consumerSpan.tags())
      .containsOnly(entry("kafka.topic", "myTopic"));
  }

  @Test
  public void should_call_wrapped_poll_and_close_spans_with_duration() {
    consumer.addRecord(consumerRecord);
    Consumer<String, String> tracingConsumer = kafkaTracing.consumer(consumer);
    tracingConsumer.poll(10);

    // offset changed
    assertThat(consumer.position(topicPartition)).isEqualTo(2L);

    MutableSpan consumerSpan = spans.get(0);
    assertThat(consumerSpan.kind()).isEqualTo(CONSUMER);
    assertThat(consumerSpan.name()).isEqualTo("poll");
    assertThat(consumerSpan.tags())
      .containsOnly(entry("kafka.topic", "myTopic"));
  }

  @Test
  public void should_add_new_trace_headers_if_b3_missing() {
    consumer.addRecord(consumerRecord);

    Consumer<String, String> tracingConsumer = kafkaTracing.consumer(consumer);
    ConsumerRecords<String, String> poll = tracingConsumer.poll(10);

    assertThat(poll)
      .extracting(ConsumerRecord::headers)
      .flatExtracting(KafkaTest::lastHeaders)
      .extracting(Map.Entry::getKey)
      .containsOnly("b3");

    MutableSpan consumerSpan = spans.get(0);
    assertThat(consumerSpan.kind()).isEqualTo(CONSUMER);
    assertThat(consumerSpan.parentId()).isNull();
  }

  @Test
  public void should_createChildOfTraceHeaders() {
    addB3MultiHeaders(parent, consumerRecord);
    consumer.addRecord(consumerRecord);

    Consumer<String, String> tracingConsumer = kafkaTracing.consumer(consumer);
    ConsumerRecords<String, String> poll = tracingConsumer.poll(10);

    assertThat(poll)
      .extracting(ConsumerRecord::headers)
      .flatExtracting(TracingConsumerTest::lastHeaders)
      .hasSize(1)
      .allSatisfy(e -> {
        assertThat(e.getKey()).isEqualTo("b3");
        assertThat(e.getValue()).startsWith(parent.traceIdString());
      });

    assertChildOf(spans.get(0), parent);
  }

  @Test
  public void should_create_only_one_consumer_span_per_topic_whenSharingEnabled() {
    Map<TopicPartition, Long> offsets = new HashMap<>();
    // 2 partitions in the same topic
    offsets.put(new TopicPartition(TEST_TOPIC, 0), 0L);
    offsets.put(new TopicPartition(TEST_TOPIC, 1), 0L);

    consumer.updateBeginningOffsets(offsets);
    consumer.assign(offsets.keySet());

    // create 500 messages
    for (int i = 0; i < 250; i++) {
      consumer.addRecord(new ConsumerRecord<>(TEST_TOPIC, 0, i, TEST_KEY, TEST_VALUE));
      consumer.addRecord(new ConsumerRecord<>(TEST_TOPIC, 1, i, TEST_KEY, TEST_VALUE));
    }

    Consumer<String, String> tracingConsumer = kafkaTracing.consumer(consumer);
    tracingConsumer.poll(10);

    // only one consumer span reported
    MutableSpan consumerSpan = spans.get(0);
    assertThat(consumerSpan.kind()).isEqualTo(CONSUMER);
    assertThat(consumerSpan.name()).isEqualTo("poll");
    assertThat(consumerSpan.tags())
      .containsOnly(entry("kafka.topic", "myTopic"));
  }

  @Test
  public void should_create_individual_span_per_topic_whenSharingDisabled() {
    kafkaTracing = kafkaTracing.toBuilder().singleRootSpanOnReceiveBatch(false).build();

    Map<TopicPartition, Long> offsets = new HashMap<>();
    // 2 partitions in the same topic
    offsets.put(new TopicPartition(TEST_TOPIC, 0), 0L);
    offsets.put(new TopicPartition(TEST_TOPIC, 1), 0L);

    consumer.updateBeginningOffsets(offsets);
    consumer.assign(offsets.keySet());

    // create 500 messages
    for (int i = 0; i < 250; i++) {
      consumer.addRecord(new ConsumerRecord<>(TEST_TOPIC, 0, i, TEST_KEY, TEST_VALUE));
      consumer.addRecord(new ConsumerRecord<>(TEST_TOPIC, 1, i, TEST_KEY, TEST_VALUE));
    }

    Consumer<String, String> tracingConsumer = kafkaTracing.consumer(consumer);
    tracingConsumer.poll(10);

    // 500 consumer spans!
    for (int i = 0; i < 500; i++) {
      MutableSpan consumerSpan = spans.get(0);
      assertThat(consumerSpan.kind()).isEqualTo(CONSUMER);
      assertThat(consumerSpan.name()).isEqualTo("poll");
      assertThat(consumerSpan.tags())
        .containsOnly(entry("kafka.topic", "myTopic"));
    }
  }
}
