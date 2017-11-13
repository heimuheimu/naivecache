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

package com.heimuheimu.naivecache.memcached.advance;

import com.heimuheimu.naivecache.memcached.NaiveMemcachedClient;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientListener;
import com.heimuheimu.naivecache.net.SocketConfiguration;

import java.util.Map;
import java.util.Set;

/**
 * 使用单个 Memcached 服务的扩展客户端抽象类，由子类实现具体客户端的创建及释放。
 *
 * <p><strong>说明：</strong>{@code AdvanceMemcachedClient} 的实现类必须是线程安全的。</p>
 *
 * @author heimuheimu
 */
public abstract class AdvanceMemcachedClient implements NaiveMemcachedClient {

    /**
     * Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    protected final String host;

    /**
     * Socket 配置信息，如果为 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     */
    protected final SocketConfiguration configuration;

    /**
     * Memcached 操作超时时间，单位：毫秒，不能小于等于0
     */
    protected final int timeout;

    /**
     * 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     */
    protected final int compressionThreshold;

    /**
     * Memcached 客户端事件监听器，允许为 {@code null}
     */
    protected final NaiveMemcachedClientListener naiveMemcachedClientListener;

    /**
     * 构造一个使用单个 Memcached 服务的扩展客户端
     * <p>该客户端的操作超时时间设置为 1 秒，最小压缩字节数设置为 64 KB</p>
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    public AdvanceMemcachedClient(String host) {
        this (host, null, 1000, 64 * 1024, null);
    }

    /**
     * 构造一个使用单个 Memcached 服务的扩展客户端
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @param configuration Socket 配置信息，如果为 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param naiveMemcachedClientListener Memcached 客户端事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 timeout 小于等于0
     * @throws IllegalArgumentException 如果 compressionThreshold 小于等于0
     */
    public AdvanceMemcachedClient(String host, SocketConfiguration configuration, int timeout, int compressionThreshold,
                                  NaiveMemcachedClientListener naiveMemcachedClientListener) throws IllegalArgumentException {

        if (timeout <= 0) {
            throw new IllegalArgumentException("Create " + this.getClass().getSimpleName() + " failed. Timeout could not be equal or less than 0. Host: `" + host + "`. Configuration: `"
                    + configuration + "`. Timeout: `" + timeout + "`. compressionThreshold: `" + compressionThreshold + "`. clientListener: `"
                    + naiveMemcachedClientListener + "`.");
        }
        if (compressionThreshold <= 0) {
            throw new IllegalArgumentException("Create " + this.getClass().getSimpleName() + " failed. CompressionThreshold could not be equal or less than 0. Host: `"
                    + host + "`. Configuration: `" + configuration + "`. Timeout: `" + timeout + "`. compressionThreshold: `"
                    + compressionThreshold + "`. clientListener: `" + naiveMemcachedClientListener + "`.");
        }
        this.host = host;
        this.configuration = configuration;
        this.timeout = timeout;
        this.compressionThreshold = compressionThreshold;
        this.naiveMemcachedClientListener = naiveMemcachedClientListener;
    }

    /**
     * 获得一个可用的 Memcached 客户端，如果当前没有可用的客户端，则返回 {@code null}
     * <p><b>注意：该方法不允许抛出任何异常</b></p>
     *
     * @return 可用的 Memcached 客户端，如果当前没有可用的客户端，则返回 {@code null}
     */
    protected abstract NaiveMemcachedClient getClient();

    /**
     * 在 Memcached 单次操作结束后，将会回调当前方法，进行客户端释放
     *
     * @param client 进行 Memcached 操作的客户端
     */
    protected abstract void releaseClient(NaiveMemcachedClient client);

    @Override
    public <T> T get(String key) {
        NaiveMemcachedClient client = getClient();
        if (client != null) {
            try {
                return client.get(key);
            } finally {
                releaseClient(client);
            }
        } else {
            return null;
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keySet) {
        NaiveMemcachedClient client = getClient();
        if (client != null) {
            try {
                return client.multiGet(keySet);
            } finally {
                releaseClient(client);
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean set(String key, Object value) {
        return set(key, value, 0);
    }

    @Override
    public boolean set(String key, Object value, int expiry) {
        NaiveMemcachedClient client = getClient();
        if (client != null) {
            try {
                return client.set(key, value, expiry);
            } finally {
                releaseClient(client);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        NaiveMemcachedClient client = getClient();
        if (client != null) {
            try {
                return client.delete(key);
            } finally {
                releaseClient(client);
            }
        } else {
            return false;
        }
    }

    @Override
    public String getHost() {
        return host;
    }

}
