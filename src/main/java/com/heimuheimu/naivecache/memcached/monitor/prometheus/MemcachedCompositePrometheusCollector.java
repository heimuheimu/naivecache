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

import com.heimuheimu.naivemonitor.prometheus.PrometheusCollector;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;

import java.util.ArrayList;
import java.util.List;

/**
 * Memcached 客户端信息复合采集器，该采集器将会收集以下采集器的信息：
 * <ul>
 *     <li>{@link MemcachedExecutionPrometheusCollector} Memcached 客户端使用的执行信息采集器</li>
 *     <li>{@link MemcachedSocketPrometheusCollector} Memcached 客户端使用的 Socket 读、写信息采集器</li>
 *     <li>{@link MemcachedCompressionPrometheusCollector} Memcached 客户端使用的压缩信息采集器</li>
 *     <li>{@link MemcachedThreadPoolPrometheusCollector} Memcached 客户端使用的线程池信息采集器</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class MemcachedCompositePrometheusCollector implements PrometheusCollector {

    /**
     * Memcached 客户端使用的执行信息采集器
     */
    private final MemcachedExecutionPrometheusCollector executionCollector;

    /**
     * Memcached 客户端使用的 Socket 读、写信息采集器
     */
    private final MemcachedSocketPrometheusCollector socketCollector;

    /**
     * Memcached 客户端使用的压缩信息采集器
     */
    private final MemcachedCompressionPrometheusCollector compressionCollector;

    /**
     * Memcached 客户端使用的线程池信息采集器
     */
    private final MemcachedThreadPoolPrometheusCollector threadPoolCollector;

    /**
     * 构造一个 MemcachedCompositePrometheusCollector 实例。
     *
     * @param configurationList 配置信息列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 configurationList 为 {@code null} 或空，将会抛出此异常
     */
    public MemcachedCompositePrometheusCollector(List<MemcachedPrometheusCollectorConfiguration> configurationList)
            throws IllegalArgumentException{
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `MemcachedCompositePrometheusCollector` failed: `configurationList could not be empty`.");
        }
        this.executionCollector = new MemcachedExecutionPrometheusCollector(configurationList);
        this.socketCollector = new MemcachedSocketPrometheusCollector(configurationList);
        this.compressionCollector = new MemcachedCompressionPrometheusCollector();
        this.threadPoolCollector = new MemcachedThreadPoolPrometheusCollector();
    }

    @Override
    public List<PrometheusData> getList() {
        List<PrometheusData> dataList = new ArrayList<>();
        dataList.addAll(executionCollector.getList());
        dataList.addAll(socketCollector.getList());
        dataList.addAll(compressionCollector.getList());
        dataList.addAll(threadPoolCollector.getList());
        return dataList;
    }
}
