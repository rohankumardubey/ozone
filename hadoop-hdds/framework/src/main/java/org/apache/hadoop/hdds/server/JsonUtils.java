/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.hadoop.hdds.server;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * JSON Utility functions used in ozone.
 */
public final class JsonUtils {

  // Reuse ObjectMapper instance for improving performance.
  // ObjectMapper is thread safe as long as we always configure instance
  // before use.
  private static final ObjectMapper MAPPER;
  private static final ObjectWriter WRITTER;
  static {
    MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    WRITTER = MAPPER.writerWithDefaultPrettyPrinter();
  }

  private JsonUtils() {
    // Never constructed
  }

  public static String toJsonStringWithDefaultPrettyPrinter(Object obj)
      throws IOException {
    return WRITTER.writeValueAsString(obj);
  }

  public static String toJsonString(Object obj) throws IOException {
    return MAPPER.writeValueAsString(obj);
  }

  /**
   * Deserialize a list of elements from a given string,
   * each element in the list is in the given type.
   *
   * @param str json string.
   * @param elementType element type.
   * @return List of elements of type elementType
   * @throws IOException
   */
  public static List<?> toJsonList(String str, Class<?> elementType)
      throws IOException {
    CollectionType type = MAPPER.getTypeFactory()
        .constructCollectionType(List.class, elementType);
    return MAPPER.readValue(str, type);
  }
}
