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

import com.heimuheimu.naivecache.memcached.monitor.CompressionMonitorFactory;
import com.heimuheimu.naivemonitor.monitor.CompressionMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractCompressionPrometheusCollector;

import java.util.Collections;
import java.util.List;

/**
 * Memcached 客户端使用的压缩信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naivecache_memcached_compression_count 相邻两次采集周期内已执行的压缩次数</li>
 *     <li>naivecache_memcached_reduce_bytes 相邻两次采集周期内通过压缩节省的字节总数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class MemcachedCompressionPrometheusCollector extends AbstractCompressionPrometheusCollector {

    @Override
    protected String getMetricPrefix() {
        return "naivecache_memcached";
    }

    @Override
    protected List<CompressionMonitor> getMonitorList() {
        return Collections.singletonList(CompressionMonitorFactory.get());
    }

    @Override
    protected String getMonitorId(CompressionMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        // do nothing
    }
}
