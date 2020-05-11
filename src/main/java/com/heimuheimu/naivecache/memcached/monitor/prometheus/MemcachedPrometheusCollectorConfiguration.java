/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 heimuheimu
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

package com.heimuheimu.naivecache.memcached.monitor.prometheus;

import java.util.List;

/**
 * Memcached 客户端信息 Prometheus 采集器配置。
 *
 * @author heimuheimu
 * @since 1.2
 */
public class MemcachedPrometheusCollectorConfiguration {

    /**
     * 访问的 Memcached 集群名称
     */
    private final String name;

    /**
     * Memcached 主机地址列表
     */
    private final List<String> hostList;

    /**
     * 构造一个 MemcachedPrometheusCollectorConfiguration 实例。
     *
     * @param name 访问的 Memcached 集群名称，不允许为 {@code null} 或空
     * @param hostList Memcached 主机地址列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 name 为 {@code null} 或空，将会抛出此异常
     * @throws IllegalArgumentException 如果 hostList 为 {@code null} 或空，将会抛出此异常
     */
    public MemcachedPrometheusCollectorConfiguration(String name, List<String> hostList) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Create `MemcachedPrometheusCollectorConfiguration` failed: `name could not be null or empty`.");
        }
        if (hostList == null || hostList.isEmpty()) {
            throw new IllegalArgumentException("Create `MemcachedPrometheusCollectorConfiguration` failed: `hostList could not be null or empty`.");
        }
        this.name = name;
        this.hostList = hostList;
    }

    /**
     * 获得访问的 Memcached 集群名称，该方法不会返回 {@code null}。
     *
     * @return Memcached 集群名称，不会为 {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * 获得 Memcached 主机地址列表，该方法不会返回 {@code null}。
     *
     * @return Memcached 主机地址列表，不会为 {@code null}
     */
    public List<String> getHostList() {
        return hostList;
    }

    @Override
    public String toString() {
        return "MemcachedPrometheusCollectorConfiguration{" +
                "name='" + name + '\'' +
                ", hostList=" + hostList +
                '}';
    }
}
