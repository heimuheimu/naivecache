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

package com.heimuheimu.naivecache.monitor;

import com.heimuheimu.naivemonitor.monitor.SocketMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memcached 客户端使用的 Socket 信息监控工厂类
 *
 * @author heimuheimu
 */
public class SocketMonitorFactory {

    private SocketMonitorFactory() {
        //private constructor
    }

    private static final ConcurrentHashMap<String, SocketMonitor> SOCKET_MONITOR_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * 根据 Socket 连接目标地址获得对应的 Socket 信息监控器，该方法不会返回 {@code null}
     *
     * @param host Socket 连接目标地址
     * @return Socket 连接目标地址获得对应的 Socket 信息监控器，该方法不会返回 {@code null}
     */
    public static SocketMonitor get(String host) {
        SocketMonitor monitor = SOCKET_MONITOR_MAP.get(host);
        if (monitor == null) {
            synchronized (lock) {
                monitor = SOCKET_MONITOR_MAP.get(host);
                if (monitor == null) {
                    monitor = new SocketMonitor(host);
                    SOCKET_MONITOR_MAP.put(host, monitor);
                }
            }
        }
        return monitor;
    }

    /**
     * 获得当前 Socket 信息监控工厂管理的所有 Socket 信息监控列表
     *
     * @return 当前 Socket 信息监控工厂管理的所有 Socket 信息监控列表
     */
    public static List<SocketMonitor> getAll() {
        return new ArrayList<>(SOCKET_MONITOR_MAP.values());
    }
}
