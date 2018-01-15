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

package com.heimuheimu.naivecache.memcached.monitor.falcon;

import com.heimuheimu.naivecache.constant.FalconDataCollectorConstant;
import com.heimuheimu.naivecache.memcached.monitor.SocketMonitorFactory;
import com.heimuheimu.naivemonitor.falcon.support.AbstractSocketDataCollector;
import com.heimuheimu.naivemonitor.monitor.SocketMonitor;

import java.util.List;

/**
 * Memcached 客户端使用的 Socket 信息 Falcon 监控数据采集器。该采集器采集周期为 30 秒，每次采集将会返回以下数据项：
 * <ul>
 *     <li>naivecache_socket_read_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 读取的总字节数</li>
 *     <li>naivecache_socket_avg_read_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次读取的平均字节数</li>
 *     <li>naivecache_socket_written_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 写入的总字节数</li>
 *     <li>naivecache_socket_avg_written_bytes/module=naivecache &nbsp;&nbsp;&nbsp;&nbsp; 30 秒内 Socket 每次写入的平均字节数</li>
 * </ul>
 *
 * @author heimuheimu
 */
public class SocketDataCollector extends AbstractSocketDataCollector {

    @Override
    protected List<SocketMonitor> getSocketMonitorList() {
        return SocketMonitorFactory.getAll();
    }

    @Override
    protected String getModuleName() {
        return FalconDataCollectorConstant.MODULE_NAME;
    }

    @Override
    public int getPeriod() {
        return FalconDataCollectorConstant.REPORT_PERIOD;
    }
}
