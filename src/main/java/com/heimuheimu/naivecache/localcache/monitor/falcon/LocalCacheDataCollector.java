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

package com.heimuheimu.naivecache.localcache.monitor.falcon;

import com.heimuheimu.naivecache.constant.FalconDataCollectorConstant;
import com.heimuheimu.naivecache.localcache.monitor.LocalCacheMonitor;
import com.heimuheimu.naivemonitor.falcon.FalconData;
import com.heimuheimu.naivemonitor.falcon.support.AbstractFalconDataCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地缓存 Falcon 监控数据采集器。该采集器采集周期为 30 秒，每次采集将会返回以下数据项：
 * <ul>
 *     <li>naivecache_local_error/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存操作出现异常总次数</li>
 *     <li>naivecache_local_query/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存 get 操作总次数</li>
 *     <li>naivecache_local_query_hit/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存 get 操作命中总次数</li>
 *     <li>naivecache_local_added/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存新增 Key 的总数</li>
 *     <li>naivecache_local_deleted/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内本地缓存删除 Key 的总数</li>
 *     <li>naivecache_local_size/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 当前本地缓存 Key 的总数</li>
 * </ul>
 *
 * @author heimuheimu
 */
public class LocalCacheDataCollector extends AbstractFalconDataCollector {

    private volatile long lastQueryCount = 0;

    private volatile long lastQueryHitCount = 0;

    private volatile long lastAddedCount = 0;

    private volatile long lastDeletedCount = 0;

    private volatile long lastErrorCount = 0;

    @Override
    protected String getModuleName() {
        return FalconDataCollectorConstant.MODULE_NAME;
    }

    @Override
    public int getPeriod() {
        return FalconDataCollectorConstant.REPORT_PERIOD;
    }

    @Override
    public List<FalconData> getList() {
        LocalCacheMonitor monitor = LocalCacheMonitor.getInstance();
        List<FalconData> falconDataList = new ArrayList<>();

        long queryCount = monitor.getQueryCount();
        falconDataList.add(create("_local_query", queryCount - lastQueryCount));
        lastQueryCount = queryCount;

        long queryHitCount = monitor.getQueryHitCount();
        falconDataList.add(create("_local_query_hit", queryHitCount - lastQueryHitCount));
        lastQueryHitCount = queryHitCount;

        long addedCount = monitor.getAddedCount();
        falconDataList.add(create("_local_added", addedCount - lastAddedCount));
        lastAddedCount = addedCount;

        long deletedCount = monitor.getDeletedCount();
        falconDataList.add(create("_local_deleted", deletedCount - lastDeletedCount));
        lastDeletedCount = deletedCount;

        long size = addedCount - deletedCount;
        falconDataList.add(create("_local_size", size));

        long errorCount = monitor.getErrorCount();
        falconDataList.add(create("_local_error", errorCount - lastErrorCount));
        lastErrorCount = errorCount;

        return falconDataList;
    }
}
