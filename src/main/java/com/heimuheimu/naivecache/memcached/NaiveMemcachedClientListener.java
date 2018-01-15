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

import java.util.concurrent.TimeUnit;

/**
 * Memcached 客户端事件监听器。
 * <p>
 *     <strong>说明：</strong>监听器的实现类必须是线程安全的。应优先考虑继承 {@link NaiveMemcachedClientListenerSkeleton} 骨架类进行实现，
 *     防止 {@code NaiveMemcachedClientListener} 在后续版本增加方法时，带来的编译错误。
 * </p>
 *
 * @author heimuheimu
 */
public interface NaiveMemcachedClientListener {

    /**
     * 大于该执行时间将会被定义为慢查，单位：纳秒。
     */
    long SLOW_EXECUTION_THRESHOLD = TimeUnit.NANOSECONDS.convert(50, TimeUnit.MILLISECONDS);

    /**
     * 当 Memcached 操作使用了非法 Key 时，将触发该监听事件。当 Key 符合以下情况之一，就会被认为非法 Key:
     * <ol>
     *     <li>Key 为 {@code null}，或者为空字符串</li>
     *     <li>Key 字节长度超过最大长度 {@link NaiveMemcachedClient#MAX_KEY_LENGTH} 限制</li>
     * </ol>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     */
    void onInvalidKey(NaiveMemcachedClient client, OperationType operationType, String key);

    /**
     * 当 Memcached 操作使用了非法 Value 时，将触发该监听事件。当 Value 符合以下情况之一，就会被认为非法 Value:
     * <ol>
     *     <li>Value 为 {@code null}</li>
     *     <li>Value 字节长度超过最大长度 {@link NaiveMemcachedClient#MAX_VALUE_LENGTH} 限制</li>
     *     <li>Value 不可序列化，未实现 {@link java.io.Serializable} 接口</li>
     * </ol>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     */
    void onInvalidValue(NaiveMemcachedClient client, OperationType operationType, String key);

    /**
     * 当 Memcached 操作使用了非法 Expiry 时，将触发该监听事件。当 Expiry 符合以下情况之一，就会被认为非法 Expiry:
     * <ol>
     *     <li>Expiry 值小于 0</li>
     * </ol>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     */
    void onInvalidExpiry(NaiveMemcachedClient client, OperationType operationType, String key);

    /**
     * 当 Memcached 客户端已经关闭，无法执行指定的 Memcached 操作时，将触发该监听事件。
     * <p><strong>注意：</strong>如果命令支持多 Key 形式， 例如：{@link OperationType#MULTI_GET} 命令， 则参数 {@code key} 的格式可能为：[demo_key1, demo_key2, demo_key3] </p>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     */
    void onClosed(NaiveMemcachedClient client, OperationType operationType, String key);

    /**
     * 当 Memcached 操作发生除 Key 未找到异常时，将触发该监听事件。
     * <p><strong>注意：</strong>如果命令支持多 Key 形式， 例如：{@link OperationType#MULTI_GET} 命令， 则参数 {@code key} 的格式可能为：[demo_key1, demo_key2, demo_key3] </p>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     */
    void onKeyNotFound(NaiveMemcachedClient client, OperationType operationType, String key);

    /**
     * 当 Memcached 操作发生超时异常时，将触发该监听事件。
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     */
    void onTimeout(NaiveMemcachedClient client, OperationType operationType, String key);

    /**
     * 当 Memcached 操作发生预期外异常时，将触发该监听事件。
     * <p><strong>注意：</strong>如果命令支持多 Key 形式， 例如：{@link OperationType#MULTI_GET} 命令， 则参数 {@code key} 的格式可能为：[demo_key1, demo_key2, demo_key3] </p>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     * @param errorMessage 异常信息
     */
    void onError(NaiveMemcachedClient client, OperationType operationType, String key, String errorMessage);

    /**
     * 当 Memcached 操作时间大于 {@link #SLOW_EXECUTION_THRESHOLD} 时，将触发该监听事件。
     * <p><strong>注意：</strong>如果命令支持多 Key 形式， 例如：{@link OperationType#MULTI_GET} 命令， 则参数 {@code key} 的格式可能为：[demo_key1, demo_key2, demo_key3] </p>
     *
     * @param client Memcached 客户端
     * @param operationType Memcached 命令类型
     * @param key 命令使用的 Memcached Key
     * @param executedNanoTime 操作执行时间，单位：纳秒
     */
    void onSlowExecution(NaiveMemcachedClient client, OperationType operationType, String key, long executedNanoTime);
}
