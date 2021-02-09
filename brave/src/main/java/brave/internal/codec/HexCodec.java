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
package brave.internal.codec;

import brave.internal.RecyclableBuffers;

// code originally imported from zipkin.Util
public final class HexCodec {

  /**
   * Parses a 1 to 32 character lower-hex string with no prefix into an unsigned long, tossing any
   * bits higher than 64.
   */
  public static long lowerHexToUnsignedLong(CharSequence lowerHex) {
    int length = lowerHex.length();
    if (length < 1 || length > 32) throw isntLowerHexLong(lowerHex);

    // trim off any high bits
    int beginIndex = length > 16 ? length - 16 : 0;

    return lowerHexToUnsignedLong(lowerHex, beginIndex);
  }

  /**
   * Parses a 16 character lower-hex string with no prefix into an unsigned long, starting at the
   * specified index.
   *
   * <p>This reads a trace context a sequence potentially larger than the format. The use-case is
   * reducing garbage, by re-using the input {@code value} across multiple parse operations.
   *
   * @param value the sequence that contains a lower-hex encoded unsigned long.
   * @param beginIndex the inclusive begin index: {@linkplain CharSequence#charAt(int) index} of the
   * first lower-hex character representing the unsigned long.
   */
  public static long lowerHexToUnsignedLong(CharSequence value, int beginIndex) {
    int endIndex = Math.min(beginIndex + 16, value.length());
    long result = lenientLowerHexToUnsignedLong(value, beginIndex, endIndex);
    if (result == 0) throw isntLowerHexLong(value);
    return result;
  }

  /**
   * Like {@link #lowerHexToUnsignedLong(CharSequence, int)}, but returns zero on invalid input.
   *
   * @param value the sequence that contains a lower-hex encoded unsigned long.
   * @param beginIndex the inclusive begin index: {@linkplain CharSequence#charAt(int) index} of the
   * first lower-hex character representing the unsigned long.
   * @param endIndex the exclusive end index: {@linkplain CharSequence#charAt(int) index}
   * <em>after</em> the last lower-hex character representing the unsigned long.
   */
  public static long lenientLowerHexToUnsignedLong(CharSequence value, int beginIndex,
    int endIndex) {
    long result = 0;
    int pos = beginIndex;
    while (pos < endIndex) {
      char c = value.charAt(pos++);
      result <<= 4;
      if (c >= '0' && c <= '9') {
        result |= c - '0';
      } else if (c >= 'a' && c <= 'f') {
        result |= c - 'a' + 10;
      } else {
        return 0;
      }
    }
    return result;
  }

  static NumberFormatException isntLowerHexLong(CharSequence lowerHex) {
    throw new NumberFormatException(
      lowerHex + " should be a 1 to 32 character lower-hex string with no prefix");
  }

  /** Inspired by {@code okio.Buffer.writeLong} */
  public static String toLowerHex(long v) {
    char[] data = RecyclableBuffers.parseBuffer();
    writeHexLong(data, 0, v);
    return new String(data, 0, 16);
  }

  /** Inspired by {@code okio.Buffer.writeLong} */
  public static void writeHexLong(char[] data, int pos, long v) {
    writeHexByte(data, pos + 0, (byte) ((v >>> 56L) & 0xff));
    writeHexByte(data, pos + 2, (byte) ((v >>> 48L) & 0xff));
    writeHexByte(data, pos + 4, (byte) ((v >>> 40L) & 0xff));
    writeHexByte(data, pos + 6, (byte) ((v >>> 32L) & 0xff));
    writeHexByte(data, pos + 8, (byte) ((v >>> 24L) & 0xff));
    writeHexByte(data, pos + 10, (byte) ((v >>> 16L) & 0xff));
    writeHexByte(data, pos + 12, (byte) ((v >>> 8L) & 0xff));
    writeHexByte(data, pos + 14, (byte) (v & 0xff));
  }

  static final char[] HEX_DIGITS =
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  static void writeHexByte(char[] data, int pos, byte b) {
    data[pos + 0] = HEX_DIGITS[(b >> 4) & 0xf];
    data[pos + 1] = HEX_DIGITS[b & 0xf];
  }

  HexCodec() {
  }
}
