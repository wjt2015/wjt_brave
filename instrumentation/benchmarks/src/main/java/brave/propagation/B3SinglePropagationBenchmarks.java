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
package brave.propagation;

import brave.internal.codec.HexCodec;
import brave.propagation.TraceContext.Extractor;
import brave.propagation.TraceContext.Injector;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(3)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class B3SinglePropagationBenchmarks {
  static final Propagation<String> b3 = Propagation.B3_SINGLE_STRING;
  static final Injector<Map<String, String>> b3Injector = b3.injector(Map::put);
  static final Extractor<Map<String, String>> b3Extractor = b3.extractor(Map::get);

  static final TraceContext context = TraceContext.newBuilder()
    .traceIdHigh(HexCodec.lowerHexToUnsignedLong("67891233abcdef01"))
    .traceId(HexCodec.lowerHexToUnsignedLong("2345678912345678"))
    .spanId(HexCodec.lowerHexToUnsignedLong("463ac35c9f6413ad"))
    .sampled(true)
    .build();

  static final Map<String, String> incoming128 = new LinkedHashMap<String, String>() {
    {
      put("b3", "67891233abcdef012345678912345678-463ac35c9f6413ad-1");
    }
  };

  static final Map<String, String> incoming64 = new LinkedHashMap<String, String>() {
    {
      put("b3", "2345678912345678-463ac35c9f6413ad-1");
    }
  };

  static final Map<String, String> incomingPadded = new LinkedHashMap<String, String>() {
    {
      put("b3", "00000000000000002345678912345678-463ac35c9f6413ad-1");
    }
  };

  static final Map<String, String> incomingNotSampled = new LinkedHashMap<String, String>() {
    {
      put("b3", "0"); // unsampled
    }
  };

  static final Map<String, String> incomingMalformed = new LinkedHashMap<String, String>() {
    {
      put("b3", "b970dafd-0d95-40aa-95d8-1d8725aebe40"); // not ok
    }
  };

  static final Map<String, String> nothingIncoming = Collections.emptyMap();

  @Benchmark public void inject() {
    Map<String, String> request = new LinkedHashMap<>();
    b3Injector.inject(context, request);
  }

  @Benchmark public TraceContextOrSamplingFlags extract_128() {
    return b3Extractor.extract(incoming128);
  }

  @Benchmark public TraceContextOrSamplingFlags extract_64() {
    return b3Extractor.extract(incoming64);
  }

  @Benchmark public TraceContextOrSamplingFlags extract_padded() {
    return b3Extractor.extract(incomingPadded);
  }

  @Benchmark public TraceContextOrSamplingFlags extract_nothing() {
    return b3Extractor.extract(nothingIncoming);
  }

  @Benchmark public TraceContextOrSamplingFlags extract_unsampled() {
    return b3Extractor.extract(incomingNotSampled);
  }

  @Benchmark public TraceContextOrSamplingFlags extract_malformed() {
    return b3Extractor.extract(incomingMalformed);
  }

  // Convenience main entry-point
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .addProfiler("gc")
      .include(".*" + B3SinglePropagationBenchmarks.class.getSimpleName())
      .build();

    new Runner(opt).run();
  }
}
