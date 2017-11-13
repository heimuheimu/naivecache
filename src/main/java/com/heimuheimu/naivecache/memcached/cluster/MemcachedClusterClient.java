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

import com.heimuheimu.naivecache.constant.BeanStatusEnum;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClient;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientFactory;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientListener;
import com.heimuheimu.naivecache.memcached.cluster.hash.ConsistentHashLocator;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

/**
 * Memcached 集群客户端，连接多台 Memcached 服务，根据 Key 进行 Hash 选择。
 *
 * <p><strong>说明：</strong>{@code MemcachedClusterClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class MemcachedClusterClient implements NaiveMemcachedClient {

    private static final Logger MEMCACHED_CONNECTION_LOG = LoggerFactory.getLogger("NAIVECACHE_MEMCACHED_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedClusterClient.class);

    /**
     * Memcached 客户端定位器，根据 Memcached Key 获取对应的 Memcached 客户端索引
     */
    private final MemcachedClientLocator locator = new ConsistentHashLocator();

    /**
     * Memcached multi-get 命令执行器，用于同时执行多台 Memcached 服务的 multi-get 命令
     */
    private final MultiGetExecutor multiGetExecutor = new MultiGetExecutor();

    /**
     * Memcached 地址数组，Memcached 地址由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    private final String[] hosts;

    /**
     * Memcached 客户端列表，该列表顺序、大小与 {@link #hosts} 一致
     * <p>
     *     如果某个 Memcached 客户端不可用，该客户端在列表中的值为 {@code null}
     * </p>
     */
    private final CopyOnWriteArrayList<NaiveMemcachedClient> clientList = new CopyOnWriteArrayList<>();

    /**
     * 当前可用的 Memcached 客户端列表
     */
    private final CopyOnWriteArrayList<NaiveMemcachedClient> aliveClientList = new CopyOnWriteArrayList<>();

    /**
     * 创建 Memcached 客户端所使用的 Socket 配置信息
     */
    private final SocketConfiguration configuration;

    /**
     * Memcached 操作超时时间，单位：毫秒，不能小于等于0
     */
    private final int timeout;

    /**
     * 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     */
    private final int compressionThreshold;

    /**
     * Memcached 客户端事件监听器
     */
    private final NaiveMemcachedClientListener naiveMemcachedClientListener;

    /**
     * Memcached 集群客户端事件监听器
     */
    private final MemcachedClusterClientListener memcachedClusterClientListener;

    /**
     * Memcached 客户端恢复任务是否运行
     */
    private boolean isRescueTaskRunning = false;

    /**
     * Memcached 客户端恢复任务使用的私有锁
     */
    private final Object rescueTaskLock = new Object();

    /**
     * 当前 Memcached 集群客户端实例所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.NORMAL;

    /**
     * 构造一个 Memcached 集群客户端
     *
     * @param hosts Memcached 地址数组，Memcached 地址由主机名和端口组成，":"符号分割，例如：localhost:11211。不允许为 {@code null} 或 空数组
     * @throws IllegalArgumentException 如果 Memcached 地址数组为 {@code null} 或 空数组
     * @throws IllegalStateException 如果在创建过程中所有 Memcached 服务都不可用
     */
    public MemcachedClusterClient(String[] hosts) {
        this(hosts, null, 1000, 64 * 1024, null, null);
    }

    /**
     * 构造一个 Memcached 集群客户端
     *
     * @param hosts Memcached 地址数组，Memcached 地址由主机名和端口组成，":"符号分割，例如：localhost:11211。不允许为 {@code null} 或 空数组
     * @param configuration 创建 Memcached 客户端所使用的 Socket 配置信息，允许为 {@code null}
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0，建议为 1000ms
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param naiveMemcachedClientListener Memcached 客户端事件监听器，允许为 {@code null}
     * @param memcachedClusterClientListener Memcached 集群客户端事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 Memcached 地址数组为 {@code null} 或 空数组
     * @throws IllegalStateException 如果在创建过程中所有 Memcached 服务都不可用
     */
    public MemcachedClusterClient(String[] hosts, SocketConfiguration configuration,
                                  int timeout, int compressionThreshold,
                                  NaiveMemcachedClientListener naiveMemcachedClientListener,
                                  MemcachedClusterClientListener memcachedClusterClientListener) throws IllegalArgumentException, IllegalStateException {
        if (hosts == null || hosts.length == 0) {
            throw new IllegalArgumentException("Hosts could not be empty. Hosts: " + Arrays.toString(hosts)
                    + ". SocketConfiguration: " + configuration + ". Timeout: " + timeout
                    + ". Compression threshold: " + compressionThreshold);
        }
        this.hosts = hosts;
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.naiveMemcachedClientListener = naiveMemcachedClientListener;
        this.memcachedClusterClientListener = memcachedClusterClientListener;
        for (String host : hosts) {
            boolean isSuccess = createClient(-1, host);
            if (isSuccess) {
                MEMCACHED_CONNECTION_LOG.info("Add `{}` to cluster is success. Hosts: `{}`.", host, hosts);
                if (memcachedClusterClientListener != null) {
                    try {
                        memcachedClusterClientListener.onCreated(host);
                    } catch (Exception e) {
                        LOG.error("Call MemcachedClusterClientListener#onCreated() failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            } else {
                MEMCACHED_CONNECTION_LOG.error("Add `{}` to cluster failed. Hosts: `{}`.", host, hosts);
                if (memcachedClusterClientListener != null) {
                    try {
                        memcachedClusterClientListener.onClosed(host);
                    } catch (Exception e) {
                        LOG.error("Call MemcachedClusterClientListener#onClosed() failed. Host: `" + host + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
                    }
                }
            }
        }
        if (aliveClientList.isEmpty()) {
            throw new IllegalStateException("There is no available client. Hosts: `" + Arrays.toString(hosts) + "`");
        }
        MEMCACHED_CONNECTION_LOG.info("MemcachedClusterClient has been initialized. Hosts: `{}`.", Arrays.toString(hosts));
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
                    //noinspection Java8MapApi
                    if (thisClientKeySet == null) {
                        thisClientKeySet = new HashSet<>();
                        clusterKeyMap.put(client, thisClientKeySet);
                    }
                    thisClientKeySet.add(key);
                }
            }
            if (clusterKeyMap.size() > 1) {
                Map<String, T> result = new HashMap<>();
                List<Future<Map<String, T>>> futureList = new ArrayList<>();
                for (NaiveMemcachedClient client : clusterKeyMap.keySet()) {
                    Future<Map<String, T>> future = multiGetExecutor.submit(client, clusterKeyMap.get(client));
                    if (future != null) {
                        futureList.add(future);
                    }
                }
                for (Future<Map<String, T>> future : futureList) {
                    result.putAll(future.get());
                }
                return result;
            } else if (clusterKeyMap.size() == 1) { //如果只有一个 Client，不需要使用线程池执行
                NaiveMemcachedClient singleClient = clusterKeyMap.keySet().iterator().next();
                return singleClient.multiGet(clusterKeyMap.get(singleClient));
            } else {
                return new HashMap<>();
            }
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
            return client != null && client.set(key, value, expiry);
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
            return client != null && client.delete(key);
        } catch (Exception e) {
            LOG.error("[delete] Unexpected error: `" + e.getMessage() + "`. Key: `" + key
                    + "`. Hosts: `" + Arrays.toString(hosts) + "`.", e);
            return false;
        }
    }

    @Override
    public boolean isActive() {
        return !aliveClientList.isEmpty();
    }

    @Override
    public String getHost() {
        return Arrays.toString(hosts);
    }

    /**
     * 获得当前集群客户端使用的 Memcached 地址数组，Memcached 地址由主机名和端口组成，":"符号分割，例如：localhost:11211
     *
     * @return Memcached 地址数组，Memcached 地址由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    public String[] getHosts() {
        return hosts;
    }

    /**
     * 获得 Memcached 客户端所使用的 Socket 配置信息
     *
     * @return Memcached 客户端所使用的 Socket 配置信息
     */
    public SocketConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * 获得 Memcached 操作超时时间，单位：毫秒，该值不会小于等于0
     *
     * @return Memcached 操作超时时间，单位：毫秒，该值不会小于等于0
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 获得最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     *
     * @return 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     */
    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            for (NaiveMemcachedClient aliveClient : aliveClientList) {
                try {
                    aliveClient.close();
                } catch (Exception e) {
                    LOG.error("Close client failed: `" + aliveClient + "`. Hosts: `" + getHost() + "`.", e);
                }
            }
            try {
                multiGetExecutor.close();
            } catch (Exception e) {
                LOG.error("Close MultiGetExecutor failed. Hosts: `" + getHost() + "`.", e);
            }
            MEMCACHED_CONNECTION_LOG.info("MemcachedClusterClient has been closed. Hosts: `{}`.", Arrays.toString(hosts));
        }
    }

    @Override
    public String toString() {
        return "MemcachedClusterClient{" +
                "hosts=" + Arrays.toString(hosts) +
                ", clientList=" + clientList +
                ", aliveClientList=" + aliveClientList +
                ", configuration=" + configuration +
                ", timeout=" + timeout +
                ", compressionThreshold=" + compressionThreshold +
                ", isRescueTaskRunning=" + isRescueTaskRunning +
                ", state=" + state +
                '}';
    }

    private boolean createClient(int clientIndex, String host) {
        NaiveMemcachedClient client = NaiveMemcachedClientFactory.create(host, configuration, timeout, compressionThreshold, naiveMemcachedClientListener);
        if (client != null && client.isActive()) {
            aliveClientList.add(client);
            if (clientIndex < 0) {
                clientList.add(client);
            } else {
                clientList.set(clientIndex, client);
            }
            return true;
        } else {
            if (clientIndex < 0) {
                clientList.add(null);
            } else {
                clientList.set(clientIndex, null);
            }
            return false;
        }
    }

    private NaiveMemcachedClient getClient(String key) {
        if (state != BeanStatusEnum.NORMAL) {
            LOG.warn("Could not find client for key `{}`. MemcachedClusterClient has been closed. Hosts: `{}`.", key, hosts);
            return null;
        }
        try {
            int clientIndex = locator.getIndex(key, hosts.length);
            NaiveMemcachedClient client = clientList.get(clientIndex);
            if (client != null) { //如果该 Memcached 服务已不可用，执行移除操作
                if (!client.isActive()) {
                    boolean isRemoveSuccess= aliveClientList.remove(client);
                    if (isRemoveSuccess) {
                        clientList.set(clientIndex, null);
                        if (memcachedClusterClientListener != null) {
                            try {
                                memcachedClusterClientListener.onClosed(client.getHost());
                            } catch (Exception e) {
                                LOG.error("Call MemcachedClusterClientListener#onClosed() failed. Host: `" + client.getHost() + "`.", e);
                            }
                        }
                    }
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
        if (state == BeanStatusEnum.NORMAL) {
            synchronized (rescueTaskLock) {
                if (!isRescueTaskRunning) {
                    Thread rescueThread = new Thread() {

                        @Override
                        public void run() {
                            long startTime = System.currentTimeMillis();
                            MEMCACHED_CONNECTION_LOG.info("Rescue task has been started. Cost: {}ms. Hosts: `{}`",
                                    System.currentTimeMillis() - startTime, hosts);
                            try {
                                while (state == BeanStatusEnum.NORMAL &&
                                        aliveClientList.size() < hosts.length) {
                                    for (int i = 0; i < hosts.length; i++) {
                                        if (clientList.get(i) == null) {
                                            boolean isSuccess = createClient(i, hosts[i]);
                                            if (isSuccess) {
                                                MEMCACHED_CONNECTION_LOG.info("Rescue `{}` to cluster success.", hosts[i]);
                                                if (memcachedClusterClientListener != null) {
                                                    try {
                                                        memcachedClusterClientListener.onRecovered(hosts[i]);
                                                    } catch (Exception e) {
                                                        LOG.error("Call MemcachedClusterClientListener#onRecovered() failed. Host: `" + hosts[i] + "`.", e);
                                                    }
                                                }
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
                    rescueThread.setName("naivecache-memcached-cluster-rescue-task");
                    rescueThread.setDaemon(true);
                    rescueThread.start();
                    isRescueTaskRunning = true;
                }
            }
        }
    }

}
