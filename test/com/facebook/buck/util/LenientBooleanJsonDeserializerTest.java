/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.buck.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.util.json.ObjectMappers;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.Test;

public class LenientBooleanJsonDeserializerTest {

  static class HasBoolean {
    @JsonProperty
    @JsonDeserialize(using = LenientBooleanJsonDeserializer.class)
    Boolean val;
  }

  @Test
  public void deserializesBooleans() throws Exception {
    assertFalse(ObjectMappers.readValue("{\"val\": false}", HasBoolean.class).val);
    assertTrue(ObjectMappers.readValue("{\"val\": true}", HasBoolean.class).val);
  }

  @Test
  public void deserializesStrings() throws Exception {
    assertFalse(ObjectMappers.readValue("{\"val\": \"false\"}", HasBoolean.class).val);
    assertFalse(ObjectMappers.readValue("{\"val\": \"FALSE\"}", HasBoolean.class).val);
    assertTrue(ObjectMappers.readValue("{\"val\": \"true\"}", HasBoolean.class).val);
    assertTrue(ObjectMappers.readValue("{\"val\": \"TRUE\"}", HasBoolean.class).val);
  }

  @Test
  public void deserializesNumbers() throws Exception {
    assertFalse(ObjectMappers.readValue("{\"val\": 0}", HasBoolean.class).val);
    assertTrue(ObjectMappers.readValue("{\"val\": 1}", HasBoolean.class).val);
  }

  @Test
  public void deserializesStringNumbers() throws Exception {
    assertFalse(ObjectMappers.readValue("{\"val\": \"0\"}", HasBoolean.class).val);
    assertTrue(ObjectMappers.readValue("{\"val\": \"1\"}", HasBoolean.class).val);
  }
}
