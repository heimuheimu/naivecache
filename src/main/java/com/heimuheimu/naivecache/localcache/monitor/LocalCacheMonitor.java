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

package com.heimuheimu.naivecache.localcache.monitor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 本地缓存操作信息监控器。
 *
 * @author heimuheimu
 */
public class LocalCacheMonitor {

    private static final LocalCacheMonitor INSTANCE = new LocalCacheMonitor();

    private LocalCacheMonitor() {
        //prevent construct this class
    }

    /**
     * 本地缓存 get 操作总次数
     */
    private final AtomicLong queryCount = new AtomicLong();

    /**
     * 本地缓存 get 操作命中总次数
     */
    private final AtomicLong queryHitCount = new AtomicLong();

    /**
     * 本地缓存新增 Key 的总数
     */
    private final AtomicLong addedCount = new AtomicLong();

    /**
     * 本地缓存删除 Key 的总数
     */
    private final AtomicLong deletedCount = new AtomicLong();

    /**
     * 本地缓存操作出现异常总次数
     */
    private final AtomicLong errorCount = new AtomicLong();

    /**
     * 本地缓存 get 操作总次数 +1
     */
    public void increaseQueryCount() {
        queryCount.incrementAndGet();
    }

    /**
     * 本地缓存 get 操作命中总次数 +1
     */
    public void increaseQueryHitCount() {
        queryHitCount.incrementAndGet();
    }

    /**
     * 本地缓存新增 Key 的总数 +1
     */
    public void increaseAddedCount() {
        addedCount.incrementAndGet();
    }

    /**
     * 本地缓存删除 Key 的总数 +1
     */
    public void increaseDeletedCount() {
        deletedCount.incrementAndGet();
    }

    /**
     * 本地缓存操作出现异常总次数 +1
     */
    public void increaseErrorCount() {
        errorCount.incrementAndGet();
    }

    /**
     * 获得本地缓存 get 操作总次数
     *
     * @return 本地缓存 get 操作总次数
     */
    public long getQueryCount() {
        return queryCount.get();
    }

    /**
     * 获得本地缓存 get 操作命中总次数
     *
     * @return 本地缓存 get 操作命中总次数
     */
    public long getQueryHitCount() {
        return queryHitCount.get();
    }

    /**
     * 获得本地缓存新增 Key 的总数
     *
     * @return 本地缓存新增 Key 的总数
     */
    public long getAddedCount() {
        return addedCount.get();
    }

    /**
     * 获得本地缓存删除 Key 的总数
     *
     * @return 本地缓存删除 Key 的总数
     */
    public long getDeletedCount() {
        return deletedCount.get();
    }

    /**
     * 获得本地缓存操作出现异常总次数
     *
     * @return 本地缓存操作出现异常总次数
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * 获得本地缓存操作信息监控器
     *
     * @return 本地缓存操作信息监控器
     */
    public static LocalCacheMonitor getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "LocalCacheMonitor{" +
                "queryCount=" + queryCount +
                ", queryHitCount=" + queryHitCount +
                ", addedCount=" + addedCount +
                ", deletedCount=" + deletedCount +
                ", errorCount=" + errorCount +
                '}';
    }
}
