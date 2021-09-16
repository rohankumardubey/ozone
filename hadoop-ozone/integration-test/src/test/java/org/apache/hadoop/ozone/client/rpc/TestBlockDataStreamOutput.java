/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.hadoop.ozone.client.rpc;

import io.netty.buffer.Unpooled;
import org.apache.hadoop.conf.StorageUnit;
import org.apache.hadoop.hdds.client.ReplicationType;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ChecksumType;
import org.apache.hadoop.hdds.scm.OzoneClientConfig;
import org.apache.hadoop.ozone.MiniOzoneCluster;
import org.apache.hadoop.ozone.OzoneConfigKeys;
import org.apache.hadoop.ozone.client.ObjectStore;
import org.apache.hadoop.ozone.client.OzoneClient;
import org.apache.hadoop.ozone.client.OzoneClientFactory;
import org.apache.hadoop.ozone.client.io.OzoneDataStreamOutput;
import org.apache.hadoop.ozone.container.ContainerTestHelper;
import org.apache.hadoop.ozone.container.TestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.hadoop.hdds.scm.ScmConfigKeys.HDDS_SCM_WATCHER_TIMEOUT;
import static org.apache.hadoop.hdds.scm.ScmConfigKeys.OZONE_SCM_STALENODE_INTERVAL;

/**
 * Tests BlockDataStreamOutput class.
 */
public class TestBlockDataStreamOutput {

  /**
    * Set a timeout for each test.
    */
  @Rule
  public Timeout timeout = Timeout.seconds(300);
  private static MiniOzoneCluster cluster;
  private static OzoneConfiguration conf = new OzoneConfiguration();
  private static OzoneClient client;
  private static ObjectStore objectStore;
  private static int chunkSize;
  private static int flushSize;
  private static int maxFlushSize;
  private static int blockSize;
  private static String volumeName;
  private static String bucketName;
  private static String keyString;

  /**
   * Create a MiniDFSCluster for testing.
   * <p>
   * Ozone is made active by setting OZONE_ENABLED = true
   *
   * @throws IOException
   */
  @BeforeClass
  public static void init() throws Exception {
    chunkSize = 100;
    flushSize = 2 * chunkSize;
    maxFlushSize = 2 * flushSize;
    blockSize = 2 * maxFlushSize;

    OzoneClientConfig clientConfig = conf.getObject(OzoneClientConfig.class);
    clientConfig.setChecksumType(ChecksumType.NONE);
    clientConfig.setStreamBufferFlushDelay(false);
    conf.setFromObject(clientConfig);

    conf.setTimeDuration(HDDS_SCM_WATCHER_TIMEOUT, 1000, TimeUnit.MILLISECONDS);
    conf.setTimeDuration(OZONE_SCM_STALENODE_INTERVAL, 3, TimeUnit.SECONDS);
    conf.setQuietMode(false);
    conf.setStorageSize(OzoneConfigKeys.OZONE_SCM_BLOCK_SIZE, 4,
        StorageUnit.MB);

    cluster = MiniOzoneCluster.newBuilder(conf)
        .setNumDatanodes(7)
        .setTotalPipelineNumLimit(10)
        .setBlockSize(blockSize)
        .setChunkSize(chunkSize)
        .setStreamBufferFlushSize(flushSize)
        .setStreamBufferMaxSize(maxFlushSize)
        .setStreamBufferSizeUnit(StorageUnit.BYTES)
        .build();
    cluster.waitForClusterToBeReady();
    //the easiest way to create an open container is creating a key
    client = OzoneClientFactory.getRpcClient(conf);
    objectStore = client.getObjectStore();
    keyString = UUID.randomUUID().toString();
    volumeName = "testblockoutputstream";
    bucketName = volumeName;
    objectStore.createVolume(volumeName);
    objectStore.getVolume(volumeName).createBucket(bucketName);
  }

  private String getKeyName() {
    return UUID.randomUUID().toString();
  }

  /**
   * Shutdown MiniDFSCluster.
   */
  @AfterClass
  public static void shutdown() {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  @Test
  public void testMultiChunkWrite() throws Exception {
    // write data less than 1 chunk size use streaming.
    String keyName1 = getKeyName();
    OzoneDataStreamOutput key1 = createKey(
        keyName1, ReplicationType.RATIS, 0);
    int dataLength1 = chunkSize/2;
    byte[] data1 =
        ContainerTestHelper.getFixedLengthString(keyString, dataLength1)
            .getBytes(UTF_8);
    key1.write(Unpooled.copiedBuffer(data1));
    // now close the stream, It will update the key length.
    key1.close();
    validateData(keyName1, data1);

    // write data more than 1 chunk size use streaming.
    String keyName2 = getKeyName();
    OzoneDataStreamOutput key2 = createKey(
        keyName2, ReplicationType.RATIS, 0);
    int dataLength2 = chunkSize + 50;
    byte[] data2 =
        ContainerTestHelper.getFixedLengthString(keyString, dataLength2)
            .getBytes(UTF_8);
    key2.write(Unpooled.copiedBuffer(data2));
    // now close the stream, It will update the key length.
    key2.close();
    validateData(keyName2, data2);

    // write data more than 1 block size use streaming.
    String keyName3 = getKeyName();
    OzoneDataStreamOutput key3 = createKey(
        keyName3, ReplicationType.RATIS, 0);
    int dataLength3 = blockSize + 50;
    byte[] data3 =
        ContainerTestHelper.getFixedLengthString(keyString, dataLength3)
            .getBytes(UTF_8);
    key3.write(Unpooled.copiedBuffer(data3));
    // now close the stream, It will update the key length.
    key3.close();
    validateData(keyName3, data3);
  }

  private OzoneDataStreamOutput createKey(String keyName, ReplicationType type,
      long size) throws Exception {
    return TestHelper.createStreamKey(
        keyName, type, size, objectStore, volumeName, bucketName);
  }
  private void validateData(String keyName, byte[] data) throws Exception {
    TestHelper
        .validateData(keyName, data, objectStore, volumeName, bucketName);
  }

}