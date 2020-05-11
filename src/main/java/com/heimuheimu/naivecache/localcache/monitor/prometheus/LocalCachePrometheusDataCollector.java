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


package com.heimuheimu.naivecache.localcache.monitor.prometheus;

import com.heimuheimu.naivecache.localcache.monitor.LocalCacheMonitor;
import com.heimuheimu.naivemonitor.prometheus.PrometheusCollector;
import com.heimuheimu.naivemonitor.prometheus.PrometheusData;
import com.heimuheimu.naivemonitor.prometheus.PrometheusSample;
import com.heimuheimu.naivemonitor.util.DeltaCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地缓存客户端信息采集器，采集时会返回以下数据：
 * <ul>
 *     <li>naivecache_local_cache_total_count 采集时刻本地缓存中保存的 Key 的总个数</li>
 *     <li>naivecache_local_cache_query_count 相邻两次采集周期内本地缓存执行 get 操作的次数</li>
 *     <li>naivecache_local_cache_query_hit_count 相邻两次采集周期内本地缓存执行 get 操作的命中次数</li>
 *     <li>naivecache_local_cache_added_count 相邻两次采集周期内本地缓存新增 Key 的个数</li>
 *     <li>naivecache_local_cache_deleted_count 相邻两次采集周期内本地缓存删除 Key 的个数</li>
 *     <li>naivecache_local_cache_error_count 相邻两次采集周期内本地缓存操作出现异常的错误次数</li>
 * </ul>
 *
 * @author heimuheimu
 * @since 1.2
 */
public class LocalCachePrometheusDataCollector implements PrometheusCollector {

    /**
     * 差值计算器
     */
    private final DeltaCalculator deltaCalculator = new DeltaCalculator();

    @Override
    public List<PrometheusData> getList() {
        LocalCacheMonitor monitor = LocalCacheMonitor.getInstance();
        List<PrometheusData> dataList = new ArrayList<>();
        // add naivecache_local_cache_total_count
        dataList.add(PrometheusData.buildGauge("naivecache_local_cache_total_count", "")
                .addSample(PrometheusSample.build(monitor.getAddedCount() - monitor.getDeletedCount())));
        // add naivecache_local_cache_query_count
        dataList.add(PrometheusData.buildGauge("naivecache_local_cache_query_count", "")
                .addSample(PrometheusSample.build(deltaCalculator.delta("queryCount", monitor.getQueryCount()))));
        // add naivecache_local_cache_query_hit_count
        dataList.add(PrometheusData.buildGauge("naivecache_local_cache_query_hit_count", "")
                .addSample(PrometheusSample.build(deltaCalculator.delta("queryHitCount", monitor.getQueryHitCount()))));
        // add naivecache_local_cache_added_count
        dataList.add(PrometheusData.buildGauge("naivecache_local_cache_added_count", "")
                .addSample(PrometheusSample.build(deltaCalculator.delta("addedCount", monitor.getAddedCount()))));
        // add naivecache_local_cache_deleted_count
        dataList.add(PrometheusData.buildGauge("naivecache_local_cache_deleted_count", "")
                .addSample(PrometheusSample.build(deltaCalculator.delta("deletedCount", monitor.getDeletedCount()))));
        // add naivecache_local_cache_error_count
        dataList.add(PrometheusData.buildGauge("naivecache_local_cache_error_count", "")
                .addSample(PrometheusSample.build(deltaCalculator.delta("errorCount", monitor.getErrorCount()))));
        return dataList;
    }
}
