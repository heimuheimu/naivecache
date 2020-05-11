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

import com.heimuheimu.naivecache.memcached.monitor.ExecutionMonitorFactory;
import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.prometheus.support.AbstractExecutionPrometheusCollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Memcached 客户端使用的执行信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naivecache_memcached_exec_count{name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Memcached 操作执行次数</li>
 *     <li>naivecache_memcached_exec_peak_tps_count{name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内每秒最大 Memcached 操作执行次数</li>
 *     <li>naivecache_memcached_avg_exec_time_millisecond{name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内单次 Memcached 操作平均执行时间，单位：毫秒</li>
 *     <li>naivecache_memcached_max_exec_time_millisecond{name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内单次 Memcached 操作最大执行时间，单位：毫秒</li>
 *     <li>naivecache_memcached_exec_error_count{errorCode="-1",errorType="Timeout",name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Memcached 操作出现超时的错误次数</li>
 *     <li>naivecache_memcached_exec_error_count{errorCode="-2",errorType="KeyNotFound",name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Memcached 操作出现 Key 不存在的错误次数</li>
 *     <li>naivecache_memcached_exec_error_count{errorCode="-3",errorType="MemcachedError",name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Memcached 操作出现异常的错误次数</li>
 *     <li>naivecache_memcached_exec_error_count{errorCode="-4",errorType="SlowExecution",name="$memcachedName",remoteAddress="$remoteAddress"} 相邻两次采集周期内 Memcached 操作出现执行过慢的错误次数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class MemcachedExecutionPrometheusCollector extends AbstractExecutionPrometheusCollector {

    /**
     * Memcached 客户端使用的操作执行信息监控器列表，不会为 {@code null} 或空，仅允许在构造函数中修改
     */
    private final List<ExecutionMonitor> monitorList;

    /**
     * 操作执行信息监控器访问的 Memcached 集群名称，与 {@link #monitorList} 一一对应， 不会为 {@code null} 或空，仅允许在构造函数中修改
     */
    private final List<String> nameList;

    /**
     * 操作执行信息监控器连接的 Memcached 主机地址列表，与 {@link #monitorList} 一一对应， 不会为 {@code null} 或空，仅允许在构造函数中修改
     */
    private final List<String> hostList;

    /**
     * 构造一个 MemcachedExecutionPrometheusCollector 实例。
     *
     * @param configurationList 配置信息列表，不允许为 {@code null} 或空
     * @throws IllegalArgumentException 如果 configurationList 为 {@code null} 或空，将会抛出此异常
     */
    public MemcachedExecutionPrometheusCollector(List<MemcachedPrometheusCollectorConfiguration> configurationList) throws IllegalArgumentException {
        if (configurationList == null || configurationList.isEmpty()) {
            throw new IllegalArgumentException("Create `MemcachedExecutionPrometheusCollector` failed: `configurationList could not be empty`.");
        }
        monitorList = new ArrayList<>();
        nameList = new ArrayList<>();
        hostList = new ArrayList<>();
        for (MemcachedPrometheusCollectorConfiguration configuration : configurationList) {
            for (String host : configuration.getHostList()) {
                monitorList.add(ExecutionMonitorFactory.get(host));
                nameList.add(configuration.getName());
                hostList.add(host);
            }
        }
    }

    @Override
    protected String getMetricPrefix() {
        return "naivecache_memcached";
    }

    @Override
    protected Map<Integer, String> getErrorTypeMap() {
        Map<Integer, String> errorTypeMap = new HashMap<>();
        errorTypeMap.put(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT, "Timeout");
        errorTypeMap.put(ExecutionMonitorFactory.ERROR_CODE_KEY_NOT_FOUND, "KeyNotFound");
        errorTypeMap.put(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR, "MemcachedError");
        errorTypeMap.put(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION, "SlowExecution");
        return errorTypeMap;
    }

    @Override
    protected List<ExecutionMonitor> getMonitorList() {
        return monitorList;
    }

    @Override
    protected String getMonitorId(ExecutionMonitor monitor, int index) {
        return String.valueOf(index);
    }

    @Override
    protected void afterAddSample(int monitorIndex, PrometheusData data, PrometheusSample sample) {
        sample.addSampleLabel("name", nameList.get(monitorIndex))
                .addSampleLabel("remoteAddress", hostList.get(monitorIndex));
    }
}
