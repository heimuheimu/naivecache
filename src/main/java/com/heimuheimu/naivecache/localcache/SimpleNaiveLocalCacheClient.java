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

package com.heimuheimu.naivecache.localcache;

import com.heimuheimu.naivecache.constant.BeanStatusEnum;
import com.heimuheimu.naivecache.localcache.monitor.LocalCacheMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link ConcurrentHashMap} 实现的本地缓存客户端
 *
 * @author heimuheimu
 */
public class SimpleNaiveLocalCacheClient implements NaiveLocalCacheClient, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNaiveLocalCacheClient.class);

    private final ConcurrentHashMap<String, LocalCacheEntity> cacheMap = new ConcurrentHashMap<>();

    private final LocalCacheMonitor monitor = LocalCacheMonitor.getInstance();

    private final int maxCacheSize;

    private CleanThread cleanThread;

    private BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 构造一个本地缓存客户端，允许同时存在的最大缓存数量为一百万
     */
    public SimpleNaiveLocalCacheClient() {
        this(1000000);
    }

    /**
     * 构造一个本地缓存客户端
     *
     * @param maxCacheSize 允许同时存在的最大缓存数量
     */
    public SimpleNaiveLocalCacheClient(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * 执行本地缓存客户单初始化操作
     */
    public synchronized void init() {
        cleanThread = new CleanThread();
        cleanThread.setName("NaiveLocalCacheCleanThread");
        cleanThread.setDaemon(true);
        cleanThread.start();
    }

    /**
     * 执行本地缓存客户单关闭操作
     */
    @Override
    public synchronized void close() {
        cleanThread.stopSignal = true;
        cleanThread.interrupt();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            LocalCacheEntity entity = cacheMap.get(key);
            if (entity != null && !entity.isExpired()) {
                monitor.getQueryHitCount();
                return (T) entity.getValue();
            }
        } catch (Exception e) {
            //should not happen
            LOGGER.error("Get value from local cache failed. key: `" + key + "`.", e);
            monitor.increaseErrorCount();
        } finally {
            monitor.increaseQueryCount();
        }
        return null;
    }

    @Override
    public <T> void set(String key, T value, int expiredTime) {
        try {
            if (key == null || key.isEmpty() || value == null) { // 如果Key为空或值为null
                monitor.increaseErrorCount();
                return;
            }
            if (expiredTime <= 0) { // 没有正确设置过期时间
                expiredTime = 30;
            }
            if (expiredTime > 86400) { //过期最大时间为一天
                expiredTime = 86400;
            }
            if (cacheMap.size() > maxCacheSize) { // 当前缓存数已经超过限制的最大值
                LOGGER.error("Local cache is reached max size: " + maxCacheSize);
                monitor.increaseErrorCount();
                return;
            }
            LocalCacheEntity entity = new LocalCacheEntity(value, expiredTime);
            cacheMap.put(key, entity);
            monitor.increaseAddedCount();
        } catch (Exception e) {
            //should not happen
            LOGGER.error("Set value to local cache failed. key: `" + key + "`. value: `"
                    + value + "`. expiredTime: `" + expiredTime + "`.", e);
            monitor.increaseErrorCount();
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isEmpty()) { // 如果Key为空或值为null
            monitor.increaseErrorCount();
            return;
        }
        if (cacheMap.remove(key) != null) {
            monitor.increaseDeletedCount();
        }
    }

    private class CleanThread extends Thread {

        private volatile boolean stopSignal = false;

        @Override
        public void run() {
            while (!stopSignal) {
                try {
                    for (String key : cacheMap.keySet()) {
                        LocalCacheEntity entity = cacheMap.get(key);
                        if (entity != null) {
                            if (entity.isExpired()) {
                                delete(key);
                            }
                        }
                    }
                } catch (Exception e) {
                    //should not happen
                    LOGGER.error("LocalCache clean task execute failed.", e);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }

    }

    private static class LocalCacheEntity {

        private final Object value;

        private final int expiredTime;

        private final long timestamp;

        private LocalCacheEntity(Object value, int expiredTime) {
            this.value = value;
            this.expiredTime = expiredTime;
            this.timestamp = System.currentTimeMillis();
        }

        private Object getValue() {
            return value;
        }

        private boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > (expiredTime * 1000);
        }

    }
}
