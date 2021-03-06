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
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientFactory;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientListener;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接单个 Memcached 服务的一次性 Memcached 客户端。
 *
 * <h3>特性</h3>
 * <blockquote>
 *     每次 Memcached 操作都会新建立 Socket 连接，在操作结束后关闭该连接。
 * </blockquote>
 *
 * <h3>适用场景</h3>
 * <blockquote>
 *     连接单台 Memcached 服务器，Memcached 操作频次很低，例如几秒钟发起一次 Memcached 访问。
 * </blockquote>
 *
 * <p><strong>说明：</strong>{@code OneTimeMemcachedClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class OneTimeMemcachedClient extends AdvanceMemcachedClient {

    /**
     * 日志输出
     */
    private static final Logger LOG = LoggerFactory.getLogger(OneTimeMemcachedClient.class);

    /**
     * 构造一个使用单个 Memcached 服务的一次性客户端<br>
     * <p>该客户端的操作超时时间设置为 1 秒，最小压缩字节数设置为 64 KB</p>
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    public OneTimeMemcachedClient(String host) {
        super(host);
    }

    /**
     * 构造一个使用单个 Memcached 服务的一次性客户端
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @param configuration Socket 配置信息，如果为 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param naiveMemcachedClientListener Memcached 客户端事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 timeout 小于等于0
     * @throws IllegalArgumentException 如果 compressionThreshold 小于等于0
     */
    public OneTimeMemcachedClient(String host, SocketConfiguration configuration, int timeout, int compressionThreshold,
                                  NaiveMemcachedClientListener naiveMemcachedClientListener) throws IllegalArgumentException {
        super(host, configuration, timeout, compressionThreshold, naiveMemcachedClientListener);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    protected NaiveMemcachedClient getClient() {
        return NaiveMemcachedClientFactory.create(host, configuration, timeout, compressionThreshold, naiveMemcachedClientListener);
    }

    @Override
    protected void releaseClient(NaiveMemcachedClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                LOG.error("Close client `" + client.getHost() + "` failed.", e);
            }
        }
    }

    @Override
    public void close() {
        //do nothing
    }

}
