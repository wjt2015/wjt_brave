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
package brave.kafka.streams;

import brave.Span;
import brave.propagation.CurrentTraceContext.Scope;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.junit.Test;

import static brave.test.ITRemote.BAGGAGE_FIELD;
import static brave.test.ITRemote.BAGGAGE_FIELD_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class KafkaStreamsTracingTest extends KafkaStreamsTest {
  @Test
  public void nextSpan_uses_current_context() {
    ProcessorContext fakeProcessorContext = processorContextSupplier.apply(new RecordHeaders());
    Span child;
    try (Scope ws = tracing.currentTraceContext().newScope(parent)) {
      child = kafkaStreamsTracing.nextSpan(fakeProcessorContext);
    }
    child.finish();

    assertThat(child.context().parentIdString())
        .isEqualTo(parent.spanIdString());
  }

  @Test
  public void nextSpan_should_create_span_if_no_headers() {
    ProcessorContext fakeProcessorContext = processorContextSupplier.apply(new RecordHeaders());
    assertThat(kafkaStreamsTracing.nextSpan(fakeProcessorContext)).isNotNull();
  }

  @Test
  public void nextSpan_should_tag_app_id_and_task_id() {
    ProcessorContext fakeProcessorContext = processorContextSupplier.apply(new RecordHeaders());
    kafkaStreamsTracing.nextSpan(fakeProcessorContext).start().finish();

    assertThat(spans.get(0).tags())
      .containsOnly(
        entry("kafka.streams.application.id", TEST_APPLICATION_ID),
        entry("kafka.streams.task.id", TEST_TASK_ID));
  }

  @Test
  public void processorSupplier_should_tag_app_id_and_task_id() {
    Processor<String, String> processor = fakeProcessorSupplier.get();
    processor.init(processorContextSupplier.apply(new RecordHeaders()));
    processor.process(TEST_KEY, TEST_VALUE);

    assertThat(spans.get(0).tags())
      .containsOnly(
        entry("kafka.streams.application.id", TEST_APPLICATION_ID),
        entry("kafka.streams.task.id", TEST_TASK_ID));
  }

  @Test
  public void processorSupplier_should_add_baggage_field() {
    ProcessorSupplier<String, String> processorSupplier =
      kafkaStreamsTracing.processor(
        "forward-1", () ->
          new AbstractProcessor<String, String>() {
            @Override
            public void process(String key, String value) {
              assertThat(BAGGAGE_FIELD.getValue(currentTraceContext.get())).isEqualTo("user1");
            }
          });
    Headers headers = new RecordHeaders().add(BAGGAGE_FIELD_KEY, "user1".getBytes());
    Processor<String, String> processor = processorSupplier.get();
    processor.init(processorContextSupplier.apply(headers));
    processor.process(TEST_KEY, TEST_VALUE);
  }

  @Test
  public void transformSupplier_should_tag_app_id_and_task_id() {
    Transformer<String, String, KeyValue<String, String>> processor = fakeTransformerSupplier.get();
    processor.init(processorContextSupplier.apply(new RecordHeaders()));
    processor.transform(TEST_KEY, TEST_VALUE);

    assertThat(spans.get(0).tags())
      .containsOnly(
        entry("kafka.streams.application.id", TEST_APPLICATION_ID),
        entry("kafka.streams.task.id", TEST_TASK_ID));
  }

  @Test
  public void valueTransformSupplier_should_tag_app_id_and_task_id() {
    ValueTransformer<String, String> processor = fakeValueTransformerSupplier.get();
    processor.init(processorContextSupplier.apply(new RecordHeaders()));
    processor.transform(TEST_VALUE);

    assertThat(spans.get(0).tags())
      .containsOnly(
        entry("kafka.streams.application.id", TEST_APPLICATION_ID),
        entry("kafka.streams.task.id", TEST_TASK_ID));
  }

  @Test
  public void valueTransformWithKeySupplier_should_tag_app_id_and_task_id() {
    ValueTransformerWithKey<String, String, String> processor =
      fakeValueTransformerWithKeySupplier.get();
    processor.init(processorContextSupplier.apply(new RecordHeaders()));
    processor.transform(TEST_KEY, TEST_VALUE);

    assertThat(spans.get(0).tags())
      .containsOnly(
        entry("kafka.streams.application.id", TEST_APPLICATION_ID),
        entry("kafka.streams.task.id", TEST_TASK_ID));
  }
}
