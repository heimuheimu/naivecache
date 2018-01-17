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
 * Memcached 客户端，可访问以下网站来获得更多 Memcached 信息：<a href="https://www.memcached.org">https://www.memcached.org</a>。
 *
 * <p><strong>说明：</strong>{@code NaiveMemcachedClient} 的实现类必须是线程安全的，并且所有的操作都不能抛出异常。</p>
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
     * 根据 Key 获取在 Memcached 中存储的值，如果不存在或者发生异常，则会返回 {@code null}。
     * Memcached get 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#get">
     *     https://github.com/memcached/memcached/wiki/Commands#get
     * </a>
     *
     * <p><strong>注意：</strong>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}</p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param <T> Value 类型
     * @return key 对应的值，，如果找不到或者发生异常，则会返回 {@code null}
     */
    <T> T get(String key);

    /**
     * 根据 Key 列表批量获取在 Memcached 中存储的值，找到的 Key 将会把对应的 Key 和结果放入 Map 中，
     * 未找到或发生异常的 Key 不会出现在返回 Map 中，该方法不会返回 {@code null}。
     * Memcached get 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#get">
     *     https://github.com/memcached/memcached/wiki/Commands#get
     * </a>
     *
     * <p><strong>注意：</strong>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}</p>
     *
     * @param keySet Memcached key 列表，Key 字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param <T> Value 类型
     * @return Key 列表对应的 Memcached 缓存值 Map，不会返回 {@code null}
     */
    <T> Map<String, T> multiGet(Set<String> keySet);

    /**
     * 将 Key 和 Value 添加至 Memcached 中，不指定过期时间。
     * 添加成功，返回 {@code true}，添加失败或发生异常，则返回 {@code false}。
     * <p><strong>说明：</strong>如果 Key 已在 Memcached 中存在，添加操作将失败。</p>
     * Memcached add 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#add">
     *     https://github.com/memcached/memcached/wiki/Commands#add
     * </a>
     *
     * <p>
     *     <strong>注意：</strong>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}，
     *     Value 序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}。
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param value Memcached value，序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}
     * @return 添加成功，返回 {@code true}，添加失败或发生异常，则返回 {@code false}
     */
    boolean add(String key, Object value);

    /**
     * 将 Key 和 Value 添加至 Memcached 中，并指定过期时间，单位：秒。
     * 添加成功，返回 {@code true}，添加失败或发生异常，则返回 {@code false}。
     * <p><strong>说明：</strong>如果 Key 已在 Memcached 中存在，添加操作将失败。</p>
     * Memcached add 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#add">
     *     https://github.com/memcached/memcached/wiki/Commands#add
     * </a>
     *
     * <p>
     *     <strong>注意：</strong>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}，
     *     Value 序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}。
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param value Memcached value，序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}
     * @param expiry 过期时间，单位：秒，不允许小于 0
     * @return 添加成功，返回 {@code true}，添加失败或发生异常，则返回 {@code false}
     */
    boolean add(String key, Object value, int expiry);

    /**
     * 将 Key 和 Value 存储至 Memcached 中，不指定过期时间，
     * 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}。
     * Memcached set 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#set">
     *     https://github.com/memcached/memcached/wiki/Commands#set
     * </a>
     *
     * <p>
     *     <strong>注意：</strong>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}，
     *     Value 序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}。
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param value Memcached value，序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}
     * @return 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}
     */
    boolean set(String key, Object value);

    /**
     * 将 Key 和 Value 存储至 Memcached 中，并指定过期时间，单位：秒。
     * 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}。
     * Memcached set 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#set">
     *     https://github.com/memcached/memcached/wiki/Commands#set
     * </a>
     *
     * <p>
     *     <strong>注意：</strong>Key 的字节长度不应超过 {@link #MAX_KEY_LENGTH}，
     *     Value 序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}。
     * </p>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param value Memcached value，序列化后的字节长度不应超过 {@link #MAX_VALUE_LENGTH}
     * @param expiry 过期时间，单位：秒，不允许小于 0
     * @return 设置成功，返回 {@code true}，设置失败或发生异常，则返回 {@code false}
     */
    boolean set(String key, Object value, int expiry);

    /**
     * 将 Memcached 中对应的 Key 删除，
     * 删除成功，返回 {@code true}，删除失败或发生异常，则返回 {@code false}。
     * Memcached delete 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#delete">
     *     https://github.com/memcached/memcached/wiki/Commands#delete
     * </a>
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
     * Memcached incr/decr 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/Commands#incrdecr">
     *     https://github.com/memcached/memcached/wiki/Commands#incrdecr
     * </a>
     *
     * <p>
     *     <strong>说明：</strong>过期时间从 Key 第一次被初始化后开始计算，后续原子加（或减）操作不会对过期时间进行重新设置，
     *     如果需要持续更新过期时间，应结合 {@link #touch(String, int)} 命令使用。
     * </p>
     *
     * <p><strong>注意：</strong>{@link #get(String)} 无法获取执行原子加（或减）操作的 Key 对应的值，
     * 可将 {@code delta} 设置为 0 进行获取。</p>
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
     * Memcached touch 命令可参考文档：
     * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#touch-gat-and-gatq">
     *     https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#touch-gat-and-gatq
     * </a>
     *
     * @param key Memcached key，字节长度不应超过 {@link #MAX_KEY_LENGTH}
     * @param expiry 过期时间，单位：秒，不允许小于0
     */
    void touch(String key, int expiry);

    /**
     * 判断当前客户端是否处于可用状态。
     *
     * @return 当前客户端是否处于可用状态
     */
    boolean isActive();

    /**
     * 获得当前客户端所连的 Memcached 地址。
     *
     * <p><strong>说明：</strong>客户端可能支持多 Memcached 地址，输出格式由实现类自行定义。</p>
     *
     * @return 当前客户端所连的 Memcached 地址
     */
    String getHost();
}
