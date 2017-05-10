/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 heimuheimu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.heimuheimu.naivecache.memcached.cluster;

import com.heimuheimu.naivecache.memcached.NaiveMemcachedClient;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientFactory;
import com.heimuheimu.naivecache.memcached.cluster.hash.ConsistentHashLocator;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * 可使用多台 Memcached 服务的客户端
 *
 * @author heimuheimu
 */
public class MemcachedClusterClient implements NaiveMemcachedClient {

    private static final Logger MEMCACHED_CONNECTION_LOG = LoggerFactory.getLogger("NAIVECACHE_MEMCACHED_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedClusterClient.class);

    private final MemcachedClientLocator locator = new ConsistentHashLocator();

    private final MultiGetExecutor multiGetExecutor = new MultiGetExecutor();

    private final String[] hosts;

    private final CopyOnWriteArrayList<NaiveMemcachedClient> clientList = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<NaiveMemcachedClient> aliveClientList = new CopyOnWriteArrayList<>();

    private final SocketConfiguration configuration;

    private final int timeout;

    private final int compressionThreshold;

    private boolean isRescueTaskRunning = false;

    private final Object rescueTaskLock = new Object();

    public MemcachedClusterClient(String[] hosts, SocketConfiguration configuration,
                                  int timeout, int compressionThreshold) {
        if (hosts == null || hosts.length == 0) {
            throw new IllegalArgumentException("Hosts could not be empty. Hosts: " + Arrays.toString(hosts)
                    + ". SocketConfiguration: " + configuration + ". Timeout: " + timeout
                    + ". Compression threshold: " + compressionThreshold);
        }
        this.hosts = hosts;
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        for (String host : hosts) {
            boolean isSuccess = createClient(host);
            if (isSuccess) {
                MEMCACHED_CONNECTION_LOG.info("Add `{}` to cluster is success. Hosts: `{}`.", host, hosts);
            } else {
                MEMCACHED_CONNECTION_LOG.warn("Add `{}` to cluster failed. Hosts: `{}`.", host, hosts);
            }
        }
    }

    @Override
    public <T> T get(String key) {
        try {
            NaiveMemcachedClient client = getClient(key);
            if (client != null) {
                return client.get(key);
            } else {
                return null;
            }
        } catch (Exception e) {
            LOG.error("[get] Unexpected error: `" + e.getMessage() + "`. Key: `"
                    + key + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
            return null;
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keySet) {
        try {
            Map<NaiveMemcachedClient, Set<String>> clusterKeyMap = new HashMap<>();
            for (String key : keySet) {
                NaiveMemcachedClient client = getClient(key);
                if (client != null) {
                    Set<String> thisClientKeySet = clusterKeyMap.get(client);
                    if (thisClientKeySet == null) {
                        thisClientKeySet = new HashSet<>();
                        clusterKeyMap.put(client, thisClientKeySet);
                    }
                    thisClientKeySet.add(key);
                }
            }
            Map<String, T> result = new HashMap<>();
            List<Future<Map<String, T>>> futureList = new ArrayList<>();
            for (NaiveMemcachedClient client : clusterKeyMap.keySet()) {
                futureList.add(multiGetExecutor.submit(client, clusterKeyMap.get(client)));
            }
            for (Future<Map<String, T>> future : futureList) {
                result.putAll(future.get());
            }
            return result;
        } catch (Exception e) {
            LOG.error("[multi-get] Unexpected error: `" + e.getMessage() + "`. Key set: `"
                    + keySet + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean set(String key, Object value) {
        return set(key, value, 0);
    }

    @Override
    public boolean set(String key, Object value, int expiry) {
        try {
            NaiveMemcachedClient client = getClient(key);
            if (client != null) {
                return client.set(key, value, expiry);
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.error("[set] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Value: `" + value
                    + "`. Expiry: `" + expiry + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            NaiveMemcachedClient client = getClient(key);
            if (client != null) {
                return client.delete(key);
            } else {
                return false;
            }
        } catch (Exception e) {
            LOG.error("[delete] Unexpected error: `" + e.getMessage() + "`. Key: `" + key
                    + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
            return false;
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getHost() {
        return Arrays.toString(hosts);
    }

    @Override
    public void close() throws IOException {

    }

    private boolean createClient(String host) {
        NaiveMemcachedClient client = NaiveMemcachedClientFactory.create(host, configuration, timeout, compressionThreshold);
        if (client != null && client.isActive()) {
            aliveClientList.add(client);
            clientList.add(client);
            return true;
        } else {
            clientList.add(null);
            return false;
        }
    }

    private NaiveMemcachedClient getClient(String key) {
        try {
            int clientIndex = locator.getIndex(key, hosts.length);
            NaiveMemcachedClient client = clientList.get(clientIndex);
            if (client != null) { //如果该 Memcached 服务已不可用，执行移除操作
                if (!client.isActive()) {
                    aliveClientList.remove(client);
                    clientList.set(clientIndex, null);
                    client = null;
                }
            }
            //如果该 Memcached 服务不可用，从可用池里面挑选，Key将会发生漂移且不可控
            if (client == null) {
                startRescueTask(); //启动 Memcached 服务恢复线程

                int aliveClientSize = aliveClientList.size();
                if (aliveClientSize > 0) {
                    int aliveClientIndex = locator.getIndex(key, aliveClientSize);
                    client = aliveClientList.get(aliveClientIndex);
                    LOG.warn("`{}` is not available. Use backup client: `{}`. Key: `{}`.", hosts[clientIndex], client.getHost(), key);
                } else {
                    LOG.error("There is no available client. Key: `{}`", key);
                }
            }
            if (client != null) {
                LOG.debug("Choose client success. Key: `{}`. Host: `{}`.", key, client.getHost());
            }
            return client;
        } catch (Exception e) {
            LOG.error("Could not find client for key `" + key + "` due to: " + e.getMessage(), e);
            return null;
        }
    }

    private void startRescueTask() {
        synchronized (rescueTaskLock) {
            if (!isRescueTaskRunning) {
                Thread rescueThread = new Thread() {

                    @Override
                    public void run() {
                        long startTime = System.currentTimeMillis();
                        MEMCACHED_CONNECTION_LOG.info("Rescue task has been started. Hosts: `{}`",
                                System.currentTimeMillis() - startTime, hosts);
                        try {
                            while (aliveClientList.size() < hosts.length) {
                                for (int i = 0; i < hosts.length; i++) {
                                    if (clientList.get(i) == null) {
                                        boolean isSuccess = createClient(hosts[i]);
                                        if (isSuccess) {
                                            MEMCACHED_CONNECTION_LOG.info("Rescue `{}` to cluster success.", hosts[i]);
                                        } else {
                                            MEMCACHED_CONNECTION_LOG.warn("Rescue `{}` to cluster failed.", hosts[i]);
                                        }
                                    }
                                }
                                Thread.sleep(500); //delay 500ms
                            }
                            rescueOver();
                            MEMCACHED_CONNECTION_LOG.info("Rescue task has been finished. Cost: {}ms. Hosts: `{}`",
                                    System.currentTimeMillis() - startTime, hosts);
                        } catch (Exception e) {
                            rescueOver();
                            MEMCACHED_CONNECTION_LOG.info("Rescue task executed failed: `{}`. Cost: {}ms. Hosts: `{}`",
                                    e.getMessage(), System.currentTimeMillis() - startTime, hosts);
                            LOG.error("Rescue task executed failed. Hosts: `" + Arrays.toString(hosts)
                                + "`", e);
                        }
                    }

                    private void rescueOver() {
                        synchronized (rescueTaskLock) {
                            isRescueTaskRunning = false;
                        }
                    }

                };
                rescueThread.setName("MemcachedClusterClient rescue task");
                rescueThread.setDaemon(true);
                rescueThread.start();
                isRescueTaskRunning = true;
            }
        }
    }

}
