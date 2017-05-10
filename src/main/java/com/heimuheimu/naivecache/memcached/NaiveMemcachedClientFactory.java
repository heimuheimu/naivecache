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

package com.heimuheimu.naivecache.memcached;

import com.heimuheimu.naivecache.memcached.binary.DirectMemcachedClient;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memcached 客户端工厂类
 *
 * @author heimuheimu
 */
public class NaiveMemcachedClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(NaiveMemcachedClientFactory.class);

    /**
     * 创建一个 Memcached 客户端，该方法不会抛出异常，如果创建失败，则返回 {@code null}
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:9610
     * @param configuration Socket配置信息，如果传 {@code null}，将会使用{@link SocketConfiguration#DEFAULT}配置信息
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @return Memcached 客户端，有可能返回 {@code null}
     */
    public static NaiveMemcachedClient create(String host, SocketConfiguration configuration,
                                              int timeout, int compressionThreshold) {
        try {
            if (timeout <= 0) {
                LOG.warn("Invalid timeout: `{}`. Use default value: 1000ms. Host: `{}`. SocketConfiguration: `{}`. Compression threshold: `{}`.",
                        timeout, host, configuration, compressionThreshold);
                timeout = 1000;
            }
            if (compressionThreshold <= 0) {
                LOG.warn("Invalid compression threshold: `{}`. Use default value: 64KB. Host: `{}`. SocketConfiguration: `{}`. Timeout: `{}`.",
                        compressionThreshold, host, configuration, timeout);
                compressionThreshold = 64 * 1024;
            }
            NaiveMemcachedClient client = new DirectMemcachedClient(host, configuration, timeout, compressionThreshold);
            return client;
        } catch (Exception e) {
            LOG.error("Create NaiveMemcachedClient failed. Host: `" + host + "`. SocketConfiguration: `" + configuration + "`. Timeout: `"
                + timeout + "`. Compression threshold: `" + compressionThreshold + "`.", e);
            return null;
        }
    }

}
