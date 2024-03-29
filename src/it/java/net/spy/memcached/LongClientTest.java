/**
 * Copyright (C) 2006-2009 Dustin Sallings
 * Copyright (C) 2009-2011 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 * 
 * 
 * Portions Copyright (C) 2012-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * SPDX-License-Identifier: Apache-2.0
 */

package net.spy.memcached;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import net.spy.memcached.categories.StandardTests;
import net.spy.memcached.compat.SyncThread;

import static org.junit.Assert.assertEquals;

/**
 * Longer running test case.
 */
@Category(StandardTests.class)
public class LongClientTest extends ClientBaseCase {

  @Test
  public void testParallelGet() throws Throwable {
    // Get a connection with the get optimization disabled.
    client.shutdown();
    initClient(new ClientTestConnectionFactory() {
      @Override
      public MemcachedConnection
      createConnection(List<InetSocketAddress> addrs) throws IOException {
        MemcachedConnection rv = super.createConnection(addrs);
        return rv;
      }

      @Override
      public boolean shouldOptimize() {
        return false;
      }
    });

    // Throw in some seed data.
    byte[] data = new byte[2048];
    Random r = new Random();
    r.nextBytes(data);
    final int hashcode = Arrays.hashCode(data);
    final Collection<String> keys = new ArrayList<String>();
    for (int i = 0; i < 50; i++) {
      client.set("k" + i, 60, data);
      keys.add("k" + i);
    }

    // Make sure it got in.
    client.waitForQueues(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

    int cnt = SyncThread.getDistinctResultCount(25, new Callable<Integer>() {
      public Integer call() throws Exception {
        for (int i = 0; i < 25; i++) {
          Map<String, Object> m = client.getBulk(keys);
          for (String s : keys) {
            byte[] b = (byte[]) m.get(s);
            assert Arrays.hashCode(b) == hashcode : "Expected " + hashcode
                + " was " + Arrays.hashCode(b);
          }
        }
        return hashcode;
      }
    });
    assertEquals(cnt, 25);
  }
}
