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
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientListener;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 热加载 Memcached 集群客户端，允许在运行期间变更 Memcached 集群客户端配置信息，更多信息请参考 {@link #reload(String[])}
 * 和 {@link #reload(String[], SocketConfiguration, int, int, NaiveMemcachedClientListener, MemcachedClusterClientListener)} 方法说明。
 *
 * <p><strong>说明：</strong>{@code ReloadableMemcachedClusterClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class ReloadableMemcachedClusterClient implements NaiveMemcachedClient {

    /**
     * 日志输出
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReloadableMemcachedClusterClient.class);

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
     * 私有锁
     */
    private final Object lock = new Object();

    /**
     * 当前正在使用的 Memcached 集群客户端
     */
    private volatile MemcachedClusterClient memcachedClusterClient;

    /**
     * 构造一个热加载 Memcached 集群客户端，允许在运行期间变更 Memcached 集群客户端配置信息
     *
     * @param hosts Memcached 地址数组，Memcached 地址由主机名和端口组成，":"符号分割，例如：localhost:11211。不允许为 {@code null} 或 空数组
     * @param configuration 创建 Memcached 客户端所使用的 Socket 配置信息，允许为 {@code null}
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0，建议为 1000ms
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param naiveMemcachedClientListener Memcached 客户端事件监听器，允许为 {@code null}
     * @param memcachedClusterClientListener Memcached 集群客户端事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 Memcached 地址数组为 {@code null} 或 空数组
     * @throws IllegalStateException 如果在创建过程中所有 Memcached 服务都不可用
     *
     */
    public ReloadableMemcachedClusterClient(String[] hosts, SocketConfiguration configuration,
                                            int timeout, int compressionThreshold,
                                            NaiveMemcachedClientListener naiveMemcachedClientListener,
                                            MemcachedClusterClientListener memcachedClusterClientListener) throws IllegalArgumentException, IllegalStateException{
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.naiveMemcachedClientListener = naiveMemcachedClientListener;
        this.memcachedClusterClientListener = memcachedClusterClientListener;
        this.memcachedClusterClient = new MemcachedClusterClient(hosts, configuration, timeout, compressionThreshold,
                naiveMemcachedClientListener, memcachedClusterClientListener);
    }

    @Override
    public <T> T get(String key) {
        return memcachedClusterClient.get(key);
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keySet) {
        return memcachedClusterClient.multiGet(keySet);
    }

    @Override
    public boolean add(String key, Object value) {
        return memcachedClusterClient.add(key, value);
    }

    @Override
    public boolean add(String key, Object value, int expiry) {
        return memcachedClusterClient.add(key, value, expiry);
    }

    @Override
    public boolean set(String key, Object value) {
        return memcachedClusterClient.set(key, value);
    }

    @Override
    public boolean set(String key, Object value, int expiry) {
        return memcachedClusterClient.set(key, value, expiry);
    }

    @Override
    public boolean delete(String key) {
        return memcachedClusterClient.delete(key);
    }

    @Override
    public long addAndGet(String key, long delta, long initialValue, int expiry) {
        return memcachedClusterClient.addAndGet(key, delta, initialValue, expiry);
    }

    @Override
    public void touch(String key, int expiry) {
        memcachedClusterClient.touch(key, expiry);
    }

    @Override
    public boolean isActive() {
        return memcachedClusterClient.isActive();
    }

    @Override
    public String getHost() {
        return memcachedClusterClient.getHost();
    }

    @Override
    public void close() {
        memcachedClusterClient.close();
    }

    /**
     * 重载当前 Memcached 集群客户端使用 Memcached 地址数组，其它配置信息采用构造当前热加载 Memcached 集群客户端时使用的配置
     *
     * @param hosts 新的 Memcached 地址数组
     * @throws IllegalArgumentException 如果 Memcached 地址数组为 {@code null} 或 空数组
     * @throws IllegalStateException 如果在创建过程中所有 Memcached 服务都不可用
     */
    public void reload(String[] hosts) throws IllegalArgumentException, IllegalStateException {
        reload(hosts, this.configuration, this.timeout, this.compressionThreshold, this.naiveMemcachedClientListener, this.memcachedClusterClientListener);
    }

    /**
     * 使用指定的配置信息重载当前 Memcached 集群客户端
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
    public void reload(String[] hosts, SocketConfiguration configuration, int timeout, int compressionThreshold,
                       NaiveMemcachedClientListener naiveMemcachedClientListener,
                       MemcachedClusterClientListener memcachedClusterClientListener) throws IllegalArgumentException, IllegalStateException {
        if (hosts == null || hosts.length == 0) {
            LOG.error("Hosts could not be empty. Hosts: " + Arrays.toString(hosts)
                    + ". SocketConfiguration: " + configuration + ". Timeout: " + timeout
                    + ". Compression threshold: " + compressionThreshold);
            throw new IllegalArgumentException("Hosts could not be empty. Hosts: " + Arrays.toString(hosts)
                    + ". SocketConfiguration: " + configuration + ". Timeout: " + timeout
                    + ". Compression threshold: " + compressionThreshold);
        }
        synchronized (lock) {
            //create new cluster client
            MemcachedClusterClient memcachedClusterClient = new MemcachedClusterClient(hosts, configuration, timeout, compressionThreshold,
                    naiveMemcachedClientListener, memcachedClusterClientListener);

            //close previous cluster client with fixed delay
            MemcachedClusterClient prevMemcachedClusterClient = this.memcachedClusterClient;
            new CloseWithFixedDelayTask(prevMemcachedClusterClient, prevMemcachedClusterClient.getTimeout()).start();

            //use new cluster client
            this.memcachedClusterClient = memcachedClusterClient;
        }
    }

    /**
     * Memcached 集群客户端延迟关闭线程
     */
    private static class CloseWithFixedDelayTask extends Thread {

        /**
         * 需要延迟关闭的 Memcached 集群客户端
         */
        private final MemcachedClusterClient memcachedClusterClient;

        /**
         * 延迟时间，单位：秒
         */
        private final int delay;

        /**
         * 构造一个 Memcached 集群客户端延迟关闭线程
         *
         * @param memcachedClusterClient 需要关闭的 Memcached 集群客户端
         * @param delay 延迟时间，单位：毫秒
         */
        private CloseWithFixedDelayTask(MemcachedClusterClient memcachedClusterClient, int delay) {
            this.memcachedClusterClient = memcachedClusterClient;
            this.delay = delay;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {} //ignore exception
            this.memcachedClusterClient.close();
        }
    }

}
