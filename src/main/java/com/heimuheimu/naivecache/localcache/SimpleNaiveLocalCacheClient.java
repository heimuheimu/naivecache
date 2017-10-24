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

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 {@link ConcurrentHashMap} 实现的本地缓存客户端，可支持非序列化和序列化模式。
 *
 * <ul>
 *   <li>非序列化模式：缓存对象以引用的形式存储，被设置后，不允许再被修改，否则可能导致难以预期的线程错误。该模式没有序列化开销。</li>
 *   <li>
 *       序列化模式：缓存对象序列化成字节数组后存储，获取的对象允许修改，不会改变本地缓存中的值。
 *       该模式要求缓存对象继承 {@link java.io.Serializable} 接口，并存在序列化开销。
 *   </li>
 * </ul>
 *
 * <h3>数据监控</h3>
 * <blockquote>
 * 可通过 {@link LocalCacheMonitor#getInstance()} 获取本地缓存使用信息监控数据。
 * </blockquote>
 *
 * <p>{@code RpcChannel} 实例应调用 {@link #init()} 方法完成初始化，再提供服务。</p>
 *
 * <p><strong>说明：</strong>{@code SimpleNaiveLocalCacheClient} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class SimpleNaiveLocalCacheClient implements NaiveLocalCacheClient, Closeable {

    private static final AtomicLong THREAD_NUMBER_GENERATOR = new AtomicLong();

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleNaiveLocalCacheClient.class);

    /**
     * 用于存储本地缓存，{@code Map} 的 Key 为 缓存 Key，Value 为 Key 对应的缓存内容 {@code LocalCacheEntity}
     */
    private final ConcurrentHashMap<String, LocalCacheEntity> cacheMap = new ConcurrentHashMap<>();

    /**
     * {@code SimpleNaiveLocalCacheClient} 本地缓存使用信息监控器
     */
    private final LocalCacheMonitor monitor = LocalCacheMonitor.getInstance();

    /**
     * {@code SimpleNaiveLocalCacheClient} 允许存储的本地缓存最大数量
     */
    private final int maxCacheSize;

    /**
     * {@code SimpleNaiveLocalCacheClient} 是否使用序列化模式
     */
    private final boolean isSerializationMode;

    /**
     * 本地缓存过期数据后台清理线程
     */
    private CleanThread cleanThread;

    /**
     * {@code SimpleNaiveLocalCacheClient} 当前所处状态
     */
    private BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 构造一个 {@code SimpleNaiveLocalCacheClient}，使用非序列化模式，允许存储的本地缓存最大数量为一百万。
     */
    public SimpleNaiveLocalCacheClient() {
        this(false, 1000000);
    }

    /**
     * 造一个 {@code SimpleNaiveLocalCacheClient}，允许存储的本地缓存最大数量为一百万。
     *
     * @param isSerializationMode 是否使用序列化模式
     */
    public SimpleNaiveLocalCacheClient(boolean isSerializationMode) {
        this(isSerializationMode, 1000000);
    }

    /**
     * 构造一个 {@code SimpleNaiveLocalCacheClient}。
     *
     * @param isSerializationMode 是否使用序列化模式
     * @param maxCacheSize 允许存储的本地缓存最大数量
     */
    public SimpleNaiveLocalCacheClient(boolean isSerializationMode, int maxCacheSize) {
        this.isSerializationMode = isSerializationMode;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * 执行 {@code SimpleNaiveLocalCacheClient} 初始化操作。
     */
    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            state = BeanStatusEnum.NORMAL;
            try {
                cleanThread = new CleanThread();
                cleanThread.setName("naivecache-localcache-clean-task-" + THREAD_NUMBER_GENERATOR.incrementAndGet());
                cleanThread.setDaemon(true);
                cleanThread.start();
            } catch (Exception e) { //should not happen
                LOGGER.error("Initialize SimpleNaiveLocalCacheClient failed: `" + e.getMessage() + "`. MaxCacheSize: `"
                        + maxCacheSize + "`. IsSerializationMode: `" + isSerializationMode + "`.", e);
                close();
            }
        }
    }

    /**
     * 执行 {@code SimpleNaiveLocalCacheClient} 关闭操作。
     */
    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            state = BeanStatusEnum.CLOSED;
            try {
                cleanThread.stopSignal = true;
                cleanThread.interrupt();
            } catch (Exception e) { //should not happen
                LOGGER.error("Close SimpleNaiveLocalCacheClient failed: `" + e.getMessage() + "`. MaxCacheSize: `"
                        + maxCacheSize + "`. IsSerializationMode: `" + isSerializationMode + "`.", e);
            }
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (key == null || key.isEmpty()) { // 如果 Key 为空或值为 null
            LOGGER.error("Get value from local cache failed: `key could not be null or empty`.");
            monitor.increaseErrorCount();
            return null;
        }
        try {
            LocalCacheEntity entity = cacheMap.get(key);
            if (entity != null && !entity.isExpired()) {
                monitor.increaseQueryHitCount();
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
        if (key == null || key.isEmpty()) { // 如果 Key 为空或值为 null
            LOGGER.error("Set value to local cache failed: `key could not be null or empty`. Key: `{}`. Value: `{}`. ExpiredTime: `{}`.",
                    key, value, expiredTime);
            monitor.increaseErrorCount();
            return;
        }
        if (state != BeanStatusEnum.NORMAL) {
            LOGGER.error("Invalid SimpleNaiveLocalCacheClient state: `{}`.", state);
            monitor.increaseErrorCount();
        }
        try {
            if (value == null) { // 如果Key为空或值为null
                LOGGER.error("Set value to local cache failed: `value could not be null`. Key: `{}`. Value: `{}`. ExpiredTime: `{}`.",
                        key, value, expiredTime);
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
            LocalCacheEntity entity = new LocalCacheEntity(value, expiredTime, isSerializationMode);
            if (cacheMap.put(key, entity) == null) {
                monitor.increaseAddedCount();
            }
        } catch (Exception e) {
            //should not happen
            LOGGER.error("Set value to local cache failed. key: `" + key + "`. value: `"
                    + value + "`. expiredTime: `" + expiredTime + "`.", e);
            monitor.increaseErrorCount();
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isEmpty()) { // 如果 Key 为空或值为 null
            LOGGER.error("Delete value from local cache failed: `key could not be null or empty`.");
            monitor.increaseErrorCount();
            return;
        }
        if (cacheMap.remove(key) != null) {
            monitor.increaseDeletedCount();
        }
    }

    @Override
    public void touch(String key, int expiredTime) {
        if (key == null || key.isEmpty()) { // 如果 Key 为空或值为 null
            LOGGER.error("Touch key from local cache failed: `key could not be null or empty`.");
            monitor.increaseErrorCount();
            return;
        }
        LocalCacheEntity entity = cacheMap.get(key);
        if (entity != null) {
            entity.touch(expiredTime);
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

        private volatile long expiry;

        private final boolean isSerializationMode;

        private LocalCacheEntity(Object value, int expiredTime, boolean isSerializationMode) throws IOException {
            if (isSerializationMode) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(value);
                this.value = bos.toByteArray();
            } else {
                this.value = value;
            }
            this.isSerializationMode = isSerializationMode;
            touch(expiredTime);
        }

        private Object getValue() throws IOException, ClassNotFoundException {
            if (isSerializationMode) {
                ByteArrayInputStream valueBis = new ByteArrayInputStream((byte[]) value);
                ObjectInputStream ois = new ObjectInputStream(valueBis);
                return ois.readObject();
            } else {
                return value;
            }
        }

        private void touch(int expiredTime) {
            this.expiry = System.currentTimeMillis() + (expiredTime * 1000);
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }
}
