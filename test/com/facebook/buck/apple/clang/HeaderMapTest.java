/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.buck.apple.clang;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import com.facebook.buck.util.environment.Platform;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;

public class HeaderMapTest {

  private void assertThatHeaderMapsAreEqual(HeaderMap hmap1, HeaderMap hmap2) {
    hmap1.visit((str, prefix, suffix) -> assertEquals(prefix + suffix, hmap2.lookup(str)));
    if (hmap1 == hmap2) {
      return;
    }
    assertEquals(hmap1.getNumBuckets(), hmap2.getNumBuckets());
    assertEquals(hmap1.getNumEntries(), hmap2.getNumEntries());
    assertEquals(hmap1.getMaxValueLength(), hmap2.getMaxValueLength());
    hmap2.visit((str, prefix, suffix) -> assertEquals(prefix + suffix, hmap1.lookup(str)));
  }

  @Before
  public void setUp() {
    assumeTrue(Platform.detect() == Platform.MACOS || Platform.detect() == Platform.LINUX);
  }

  @Test
  public void testAddLookup() {
    int n = 11;

    HeaderMap.Builder builder = HeaderMap.builder();
    for (int i = 0; i < n; i++) {
      assertTrue(builder.add("foo" + i, Paths.get("value of foo" + i)));
    }
    assertFalse(builder.add("foo1", Paths.get("another value for foo1??")));
    HeaderMap hmap = builder.build();

    for (int i = 0; i < n; i++) {
      assertEquals("value of foo" + i, hmap.lookup("foo" + i));
    }
    for (int i = 0; i < n; i++) {
      assertEquals("value of foo" + i, hmap.lookup("FOO" + i));
    }

    assertEquals(null, hmap.lookup("BAR"));
    assertEquals(n, hmap.getNumEntries());
    assertEquals("value of foo10".length(), hmap.getMaxValueLength());

    assertThatHeaderMapsAreEqual(hmap, hmap);
  }

  @Test
  public void testAddAndToString() {
    HeaderMap.Builder builder = HeaderMap.builder();

    assertTrue(builder.add("foo", Paths.get("value of ", "foo")));
    assertTrue(builder.add("bar", Paths.get("value of ", "bar")));

    HeaderMap hmap = builder.build();
    assertEquals(
        "\"foo\" -> \"value of /foo\"\n" + "\"bar\" -> \"value of /bar\"\n", hmap.toString());
  }

  // sample hmap generated by xcode on a toy project
  final int[] testData = {
    0x686d6170, 0x00000001, 0x00000318, 0x00000003, 0x00000040, 0x0000004b,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x0000004b, 0x00000013, 0x0000004b, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000060, 0x00000013, 0x00000060,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000001, 0x00000013, 0x00000001,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
    0x69755100, 0x7070417a, 0x656c6544, 0x65746167, 0x2f00682e, 0x72657355,
    0x616d2f73, 0x65696874, 0x75616275, 0x2f746564, 0x75636f44, 0x746e656d,
    0x63582f73, 0x5065646f, 0x656a6f72, 0x2f737463, 0x7a697551, 0x6975512f,
    0x51002f7a, 0x567a6975, 0x43776569, 0x72746e6f, 0x656c6c6f, 0x00682e72,
    0x7a697551, 0x6572502d, 0x2e786966, 0x00686370
  };

  private byte[] getDataBytes(int[] data) {
    byte[] bytes = new byte[data.length * 4];
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < data.length; i++) {
      buffer.putInt(data[i]);
    }
    return bytes;
  }

  @Test
  public void testReadFromXcodeData() {
    byte[] bytes = getDataBytes(testData);

    // the original file was 904 bytes long and started with "pamh" = 70 61 6d 68
    assertEquals(904, bytes.length);
    assertEquals(0x70, bytes[0]);
    assertEquals(0x61, bytes[1]);
    assertEquals(0x6d, bytes[2]);
    assertEquals(0x68, bytes[3]);

    HeaderMap hmap = HeaderMap.deserialize(bytes);
    assertNotNull(hmap);

    assertEquals(testData[3], hmap.getNumEntries());
    assertEquals(testData[4], hmap.getNumBuckets());
    assertEquals(testData[5], hmap.getMaxValueLength());

    assertEquals(
        "\"QuizViewController.h\" -> "
            + "\"/Users/mathieubaudet/Documents/XcodeProjects/Quiz/Quiz/QuizViewController.h\"\n"
            + "\"Quiz-Prefix.pch\" -> "
            + "\"/Users/mathieubaudet/Documents/XcodeProjects/Quiz/Quiz/Quiz-Prefix.pch\"\n"
            + "\"QuizAppDelegate.h\" -> "
            + "\"/Users/mathieubaudet/Documents/XcodeProjects/Quiz/Quiz/QuizAppDelegate.h\"\n",
        hmap.toString());

    assertEquals(hmap.lookup("QuizViewController.h").length(), hmap.getMaxValueLength());
  }

  @Test
  public void testReserializeFromXcodeData() {
    byte[] bytes = getDataBytes(testData);
    HeaderMap hmap = HeaderMap.deserialize(bytes);
    assertNotNull(hmap);

    byte[] bytes1 = hmap.getBytes();
    HeaderMap hmap1 = HeaderMap.deserialize(bytes1);
    assertNotNull(hmap1);

    // Note: The ordering and number of buckets does not change when reserializing maps as we do.
    assertArrayEquals(bytes1, hmap1.getBytes());
    assertThatHeaderMapsAreEqual(hmap, hmap1);
  }

  @Test
  public void testReserializeVeryLongTable() {
    int n = 1001;

    HeaderMap.Builder builder = HeaderMap.builder();
    for (int i = 0; i < n; i++) {
      assertTrue(builder.add("foo" + i, Paths.get("value of foo", Integer.toString(i))));
    }
    HeaderMap hmap = builder.build();

    assertEquals(n, hmap.getNumEntries());
    assertEquals(2048, hmap.getNumBuckets());
    assertEquals("value of foo/1000".length(), hmap.getMaxValueLength());

    byte[] bytes = hmap.getBytes();

    HeaderMap hmap1 = HeaderMap.deserialize(bytes);
    assertNotNull(hmap1);

    assertThatHeaderMapsAreEqual(hmap, hmap1);
    assertArrayEquals(bytes, hmap1.getBytes());
  }

  private void assertThatSplitPathWorksOnPath(Path path) {
    String[] result = HeaderMap.Builder.splitPath(path);
    assertNotNull(result);
    assertEquals(2, result.length);
    assertNotNull(result[0]);
    assertNotNull(result[1]);
    assertEquals(path.toString(), result[0] + result[1]);
  }

  @Test
  public void splitPathIsCorrect() {
    assertThatSplitPathWorksOnPath(Paths.get(""));
    assertThatSplitPathWorksOnPath(Paths.get("."));
    assertThatSplitPathWorksOnPath(Paths.get("/"));
    assertThatSplitPathWorksOnPath(Paths.get("./"));
    assertThatSplitPathWorksOnPath(Paths.get("../."));
    assertThatSplitPathWorksOnPath(Paths.get("./.."));
    assertThatSplitPathWorksOnPath(Paths.get("/asdf/fdgh"));
    assertThatSplitPathWorksOnPath(Paths.get("asdf/fdgh"));
    assertThatSplitPathWorksOnPath(Paths.get("asdf/fdgh/dsfg"));
  }

  @Test
  public void addPathWorks() {
    int n = 11;

    HeaderMap.Builder builder = HeaderMap.builder();
    for (int i = 0; i < n; i++) {
      assertTrue(builder.add("foo" + i, Paths.get("bar", "file" + i)));
    }
    HeaderMap hmap = builder.build();

    for (int i = 0; i < n; i++) {
      assertEquals("bar/file" + i, hmap.lookup("foo" + i));
    }
  }

  @Test
  public void loadFactorDoesNotExceedLimit() {
    {
      int n = 384; // 384 / 512 = 0.750, ok

      HeaderMap.Builder builder = HeaderMap.builder();
      for (int i = 0; i < n; i++) {
        assertTrue(builder.add("foo" + i, Paths.get("value of foo", Integer.toString(i))));
      }
      HeaderMap hmap = builder.build();

      assertEquals(n, hmap.getNumEntries());
      assertEquals(512, hmap.getNumBuckets());
    }

    {
      int n = 385; // 385 / 512 = 0.752, should expand
      HeaderMap.Builder builder = HeaderMap.builder();
      for (int i = 0; i < n; i++) {
        assertTrue(builder.add("foo" + i, Paths.get("value of foo", Integer.toString(i))));
      }
      HeaderMap hmap = builder.build();

      assertEquals(n, hmap.getNumEntries());
      assertEquals(1024, hmap.getNumBuckets());
    }
  }
}
