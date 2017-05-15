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

package com.heimuheimu.naivecache.memcached.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memcached 命令请求信息统计
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class MemcachedMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedMonitor.class);

    private static final ConcurrentHashMap<String, MemcachedInfo> MEM_INFO_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    private MemcachedMonitor() {
        //private constructor
    }

    /**
     * 增加一个 Memcached 命令请求信息统计
     * <p>注意：该方法不会抛出任何异常</p>
     *
     * @param host Memcached 命令执行目标地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @param op Memcached 命令类型
     * @param result Memcached 命令返回结果
     * @param startTime Memcached 命令请求开始时间(nanoTime)
     */
    public static void add(String host, OperationType op, OperationResult result, long startTime) {
        try {
            MemcachedInfo memcachedInfo = get(host);
            memcachedInfo.add(op, result, startTime);
        } catch (Exception e) {
            //should not happen
            LOG.error("Unexpected error. Host: `" + host + "`, Operation: `"
                    + op + "`, Result: `" + result + "`, Start time: `" + startTime + "`.", e);
        }
    }

    public static Map<String, MemcachedInfo> get() {
        return MEM_INFO_MAP;
    }

    private static MemcachedInfo get(String host) {
        MemcachedInfo memcachedInfo = MEM_INFO_MAP.get(host);
        if (memcachedInfo == null) {
            synchronized (lock) {
                memcachedInfo = MEM_INFO_MAP.get(host);
                if (memcachedInfo == null) {
                    memcachedInfo = new MemcachedInfo(host);
                    MEM_INFO_MAP.put(host, memcachedInfo);
                }
            }
        }
        return memcachedInfo;
    }

}
