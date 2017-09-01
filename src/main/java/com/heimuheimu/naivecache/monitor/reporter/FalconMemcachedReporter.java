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

package com.heimuheimu.naivecache.monitor.reporter;

import com.heimuheimu.naivecache.monitor.ExecutionTimeInfo;
import com.heimuheimu.naivecache.monitor.memcached.MemcachedMonitor;
import com.heimuheimu.naivecache.monitor.socket.SocketMonitor;
import com.heimuheimu.naivecache.monitor.thread.ThreadPoolMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 Falcon 系统的 Memcached 命令请求统计信息监控数据上报
 *
 * @author heimuheimu
 */
public class FalconMemcachedReporter extends AbstractFalconReporter {

    private volatile long lastTpsCount = 0;

    private volatile long lastExecutionCount = 0;

    private volatile long lastTotalExecutionTime = 0;

    private volatile long lastKeyNotFoundCount = 0;

    private volatile long lastTimeoutCount = 0;

    private volatile long lastErrorCount = 0;

    private volatile long lastReadBytes = 0;

    private volatile long lastWriteBytes = 0;

    private volatile long lastRejectedCount = 0;

    public FalconMemcachedReporter(String pushUrl) {
        super(pushUrl);
    }

    public FalconMemcachedReporter(String pushUrl, Map<String, String> endpointAliasMap) {
        super(pushUrl, endpointAliasMap);
    }

    @Override
    protected List<FalconData> getPushDataList() {
        List<FalconData> dataList = new ArrayList<>();
        dataList.add(getTps());
        dataList.add(getPeakTps());
        dataList.add(getAverageExecutionTime());
        dataList.add(getMaxExecutionTime());
        dataList.add(getKeyNotFound());
        dataList.add(getTimeoutCount());
        dataList.add(getErrorCount());
        dataList.add(getReadBytes());
        dataList.add(getWriteBytes());
        dataList.add(getActiveCount());
        dataList.add(getPoolSize());
        dataList.add(getPeakPoolSize());
        dataList.add(getCorePoolSize());
        dataList.add(getMaximumPoolSize());
        dataList.add(getRejectedCount());
        return dataList;
    }

    private FalconData getTps() {
        long tpsCount = MemcachedMonitor.getGlobalInfo().getTpsInfo().getCount();
        FalconData tpsData = create();
        tpsData.metric = "naivecache_tps";
        tpsData.value = (tpsCount - lastTpsCount) / REPORT_INTERVAL_SECONDS;
        lastTpsCount = tpsCount;
        return tpsData;
    }

    private FalconData getPeakTps() {
        FalconData peakTpsData = create();
        peakTpsData.metric = "naivecache_peak_tps";
        peakTpsData.value = MemcachedMonitor.getGlobalInfo().getTpsInfo().getPeakTps();
        return peakTpsData;
    }

    private FalconData getAverageExecutionTime() {
        ExecutionTimeInfo executionTimeInfo = MemcachedMonitor.getGlobalInfo().getExecutionTimeInfo();
        long executionCount = executionTimeInfo.getCount();
        long totalExecutionTime = executionTimeInfo.getTotalExecutionTime();
        FalconData avgExecTimeData = create();
        avgExecTimeData.metric = "naivecache_avg_exec_time";
        if (executionCount > lastExecutionCount) {
            avgExecTimeData.value = (totalExecutionTime - lastTotalExecutionTime) / (executionCount - lastExecutionCount);
        } else {
            avgExecTimeData.value = 0;
        }
        lastExecutionCount = executionCount;
        lastTotalExecutionTime = totalExecutionTime;
        return avgExecTimeData;
    }

    private FalconData getMaxExecutionTime() {
        ExecutionTimeInfo executionTimeInfo = MemcachedMonitor.getGlobalInfo().getExecutionTimeInfo();
        FalconData data = create();
        data.metric = "naivecache_max_exec_time";
        data.value = executionTimeInfo.getMaxExecutionTime();
        executionTimeInfo.resetMaxExecutionTime();
        return data;
    }

    private FalconData getKeyNotFound() {
        FalconData keyNotFoundData = create();
        keyNotFoundData.metric = "naivecache_key_not_found";
        long keyNotFoundCount = MemcachedMonitor.getGlobalInfo().getTotalOpInfo().getKeyNotFound();
        keyNotFoundData.value = keyNotFoundCount - lastKeyNotFoundCount;
        lastKeyNotFoundCount = keyNotFoundCount;
        return keyNotFoundData;
    }

    private FalconData getTimeoutCount() {
        FalconData timeoutCountData = create();
        timeoutCountData.metric = "naivecache_timeout";
        long timeoutCount = MemcachedMonitor.getGlobalInfo().getTotalOpInfo().getTimeout();
        timeoutCountData.value = timeoutCount - lastTimeoutCount;
        lastTimeoutCount = timeoutCount;
        return timeoutCountData;
    }

    private FalconData getErrorCount() {
        FalconData errorCountData = create();
        errorCountData.metric = "naivecache_error";
        long errorCount = MemcachedMonitor.getGlobalInfo().getTotalOpInfo().getError();
        errorCountData.value = errorCount - lastErrorCount;
        lastErrorCount = errorCount;
        return errorCountData;
    }

    private FalconData getReadBytes() {
        FalconData readBytesData = create();
        readBytesData.metric = "naivecache_read_bytes";
        long readBytes = SocketMonitor.getGlobalInfo().getReadSize().getSize();
        readBytesData.value = readBytes - lastReadBytes;
        lastReadBytes = readBytes;
        return readBytesData;
    }

    private FalconData getWriteBytes() {
        FalconData writeBytesData = create();
        writeBytesData.metric = "naivecache_write_bytes";
        long writeBytes = SocketMonitor.getGlobalInfo().getWriteSize().getSize();
        writeBytesData.value = writeBytes - lastWriteBytes;
        lastWriteBytes = writeBytes;
        return writeBytesData;
    }

    private FalconData getActiveCount() {
        FalconData data = create();
        data.metric = "naivecache_multiGet_threadPool_active_count";
        data.value = ThreadPoolMonitor.getActiveCount();
        return data;
    }

    private FalconData getPoolSize() {
        FalconData data = create();
        data.metric = "naivecache_multiGet_threadPool_pool_size";
        data.value = ThreadPoolMonitor.getPoolSize();
        return data;
    }

    private FalconData getPeakPoolSize() {
        FalconData data = create();
        data.metric = "naivecache_multiGet_threadPool_peak_pool_size";
        data.value = ThreadPoolMonitor.getPeakPoolSize();
        return data;
    }

    private FalconData getCorePoolSize() {
        FalconData data = create();
        data.metric = "naivecache_multiGet_threadPool_core_pool_size";
        data.value = ThreadPoolMonitor.getCorePoolSize();
        return data;
    }

    private FalconData getMaximumPoolSize() {
        FalconData data = create();
        data.metric = "naivecache_multiGet_threadPool_maximum_pool_size";
        data.value = ThreadPoolMonitor.getMaximumPoolSize();
        return data;
    }

    private FalconData getRejectedCount() {
        FalconData data = create();
        data.metric = "naivecache_multiGet_threadPool_rejected_count";
        long rejectedCount = ThreadPoolMonitor.getRejectedCount();
        data.value = rejectedCount - lastRejectedCount;
        lastRejectedCount = rejectedCount;
        return data;
    }

}
