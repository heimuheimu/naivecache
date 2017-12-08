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

package com.heimuheimu.naivecache.memcached;

import java.io.Closeable;
import java.util.Map;
import java.util.Set;

/**
 * Memcached 客户端，提供 Memcached get、multi get、set、delete等常用操作
 *
 * <p><b>注意：</b>所有的客户端实现必须是线程安全的</p>
 * <p><b>注意：</b>所有的操作均不会抛出异常</p>
 *
 * @author heimuheimu
 */
public interface NaiveMemcachedClient extends Closeable {


    /**
     * Memcached Key 最大字节数：250B
     */
    int MAX_KEY_LENGTH = 250;


    /**
     * Memcached Value 最大字节数：1MB
     */
    int MAX_VALUE_LENGTH = 1024 * 1024;

    /**
     * 根据 Key 获取在 Memcached 中存储的值，如果找不到或者发生异常，则会返回 {@code null}
     *
     * <p>
     * <b>注意：</b>Key 的长度不应超过 {@link #MAX_KEY_LENGTH}
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @return key 对应的值，，如果找不到或者发生异常，则会返回 {@code null}
     */
    <T> T get(String key);

    /**
     * 根据 Key 列表批量获取在 Memcached 中存储的值，找到的 Key 将会把对应的 Key 和结果放入 Map 中，
     * 未找到或发生异常的 Key 不会出现在返回 Map 中，该方法不会返回 {@code null}
     *
     * <p>
     * <b>注意：</b>Key 的长度不应超过 {@link #MAX_KEY_LENGTH}
     * </p>
     *
     * @param keySet Memcached key 列表
     * @return Key 列表对应的 Memcached 缓存值 Map，不会返回 {@code null}
     */
    <T> Map<String, T> multiGet(Set<String> keySet);


    /**
     * 将 Key 和 Value 存储至 Memcached 中，不指定过期时间，
     * 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}
     *
     * <p><b>注意：</b>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}</p>
     * <p><b>注意：</b>Value 的字节长度不应超过 {@link #MAX_VALUE_LENGTH}</p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param value Memcached value，字节长度不应超过 {@link #MAX_VALUE_LENGTH}
     * @return 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}
     */
    boolean set(String key, Object value);

    /**
     * 将 Key 和 Value 存储至 Memcached 中，并指定过期时间，单位：秒。
     * 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}
     *
     * <p><b>注意：</b>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}</p>
     * <p><b>注意：</b>Value 的字节长度不应超过 {@link #MAX_VALUE_LENGTH}</p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param value Memcached value，字节长度不应超过 {@link #MAX_VALUE_LENGTH}
     * @param expiry 过期时间，单位：秒，不允许小于0
     * @return 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}
     */
    boolean set(String key, Object value, int expiry);

    /**
     * 将 Memcached 中对应的 Key 删除，
     * 删除成功，返回 {@code true}，删除失败或发生异常，则返回 {@code false}
     *
     * <p>
     * <b>注意：</b>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @return 删除成功，返回 {@code true}，删除失败或发生异常，则返回 {@code false}
     */
    boolean delete(String key);

    /**
     * 对 Key 对应的 long 数值执行原子加（或减）操作，并返回操作后的结果值，如果 Key 不存在，则设置并返回指定初始值。
     *
     * <p>
     *     <strong>说明：</strong>过期时间从 Key 第一次被初始化后开始计算，后续原子加（或减）操作不会对过期时间进行重新设置，
     *     如果需要持续更新过期时间，应结合 {@link #touch(String, int)} 命令使用。
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param delta 需要增加的值，如果为负数，则为减少的值
     * @param initialValue 如果 Key 不存在，设置并返回指定初始值
     * @param expiry 过期时间，单位：秒，不允许小于0
     * @return 操作后的结果值
     */
    long addAndGet(String key, long delta, long initialValue, int expiry);

    /**
     * 更新 Key 对应的过期时间。
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param expiry 过期时间，单位：秒，不允许小于0
     */
    void touch(String key, int expiry);

    /**
     * 判断当前客户端是否处于可用状态
     *
     * @return 当前客户端是否处于可用状态
     */
    boolean isActive();

    /**
     * 获得当前客户端所连的 Memcached 地址
     * <p>注意：客户端可能支持多 Memcached 地址，输出格式由实现类自行定义</p>
     *
     * @return 当前客户端所连的 Memcached 地址
     */
    String getHost();

}
