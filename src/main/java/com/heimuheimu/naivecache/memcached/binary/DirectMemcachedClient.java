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

package com.heimuheimu.naivecache.memcached.binary;

import com.heimuheimu.naivecache.memcached.NaiveMemcachedClient;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientListener;
import com.heimuheimu.naivecache.memcached.OperationType;
import com.heimuheimu.naivecache.memcached.binary.channel.MemcachedChannel;
import com.heimuheimu.naivecache.memcached.binary.command.*;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;
import com.heimuheimu.naivecache.memcached.monitor.ExecutionMonitorFactory;
import com.heimuheimu.naivecache.memcached.util.ByteUtil;
import com.heimuheimu.naivecache.net.BuildSocketException;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import com.heimuheimu.naivecache.transcoder.SimpleTranscoder;
import com.heimuheimu.naivecache.transcoder.Transcoder;
import com.heimuheimu.naivemonitor.monitor.ExecutionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Memcached 直连客户端，基于二进制协议进行通信。协议定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped
 * </a>
 *
 * <p><strong>说明：</strong>{@code MemcachedChannel} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class DirectMemcachedClient implements NaiveMemcachedClient {

    private static final Logger LOG = LoggerFactory.getLogger(DirectMemcachedClient.class);

    private static final String NO_RESPONSE_PACKET_MESSAGE = "No response packet";

    private static final Charset CHARSET_UTF8 = Charset.forName("utf-8");

    /**
     * Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    private final String host;

    /**
     * Memcached 操作超时时间，单位：毫秒，不能小于等于0
     */
    private final int timeout;

    /**
     * 基于 Memcached 二进制协议与 Memcached 服务进行数据交互的管道
     */
    private final MemcachedChannel memcachedChannel;

    /**
     * Java 对象与 Memcached 二进制协议存储的字节数组转换器
     */
    private final Transcoder transcoder;

    /**
     * Memcached 客户端事件监听器封装类，对监听器执行遇到异常进行错误捕获
     */
    private final ClientListenerWrapper clientListener;

    /**
     * 当前 Memcached 客户端使用的操作执行信息监控器
     */
    private final ExecutionMonitor executionMonitor;

    /**
     * 创建一个 Memcached 直连客户端
     * <p>该客户端的操作超时时间设置为 1 秒，最小压缩字节数设置为 64 KB</p>
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @throws IllegalArgumentException 如果目标服务器地址不符合规则，将会抛出此异常
     * @throws RuntimeException 如果创建 {@link MemcachedChannel} 过程中发生错误，将会抛出此异常
     */
    public DirectMemcachedClient(String host) throws RuntimeException {
        this(host, null, 1000, 64 * 1024, null);
    }

    /**
     * 创建一个 Memcached 直连客户端。
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @param configuration Socket 配置信息，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     * @param clientListener Memcached 客户端事件监听器，允许为 {@code null}
     * @throws IllegalArgumentException 如果 timeout 小于等于0
     * @throws IllegalArgumentException 如果 compressionThreshold 小于等于0
     * @throws IllegalArgumentException 如果 Memcached 地址不符合规则，将会抛出此异常
     * @throws BuildSocketException 如果创建 {@link MemcachedChannel} 过程中发生错误，将会抛出此异常
     */
    public DirectMemcachedClient(String host, SocketConfiguration configuration,
                                 int timeout, int compressionThreshold,
                                 NaiveMemcachedClientListener clientListener) throws IllegalArgumentException, BuildSocketException {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Create DirectMemcachedClient failed. Timeout could not be equal or less than 0. Host: `" + host + "`. Configuration: `"
                + configuration + "`. Timeout: `" + timeout + "`. compressionThreshold: `" + compressionThreshold + "`. clientListener: `"
                + clientListener + "`.");
        }
        if (compressionThreshold <= 0) {
            throw new IllegalArgumentException("Create DirectMemcachedClient failed. CompressionThreshold could not be equal or less than 0. Host: `"
                    + host + "`. Configuration: `" + configuration + "`. Timeout: `" + timeout + "`. compressionThreshold: `"
                    + compressionThreshold + "`. clientListener: `" + clientListener + "`.");
        }
        this.memcachedChannel = new MemcachedChannel(host, configuration);
        this.memcachedChannel.init();
        this.host = host;
        this.timeout = timeout;
        this.transcoder = new SimpleTranscoder(compressionThreshold);
        this.clientListener = new ClientListenerWrapper(clientListener);
        this.executionMonitor = ExecutionMonitorFactory.get(host);
    }

    @Override
    public <T> T get(String key) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[get] Key could not be empty. Key: `{}`. Host: `{}`.", key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.GET, key);
                return null;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[get] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.GET, key);
                return null;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[get] Inactive channel. Key: `{}`. Host: `{}`.", key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onClosed(this, OperationType.GET, key);
                return null;
            }
            GetCommand getCommand = new GetCommand(keyBytes);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(getCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    T value = transcoder.decode(responsePacket.getBody(), 0,
                            responsePacket.getExtrasLength() + responsePacket.getKeyLength(),
                            responsePacket.getValueLength());
                    LOG.debug("[get] Success. Key: `{}`. Value: `{}`. Host: `{}`.", key, value, host);
                    return value;
                } else {
                    if (responsePacket.isKeyNotFound()) {
                        LOG.info("[get] Key not found. Key: `{}`. Host: `{}`", key, host);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_KEY_NOT_FOUND);
                        clientListener.onKeyNotFound(this, OperationType.GET, key);
                    } else {
                        LOG.error("[get] Memcached error: `{}`. Key: `{}`. Host: `{}`.", responsePacket.getErrorMessage(), key, host);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                        clientListener.onError(this, OperationType.GET, key, responsePacket.getErrorMessage());
                    }
                    return null;
                }
            } else {
                LOG.error("[get] Empty response. Key: `{}`. Host: `{}`", key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onError(this, OperationType.GET, key, NO_RESPONSE_PACKET_MESSAGE);
                return null;
            }
        } catch(TimeoutException e) {
            LOG.error("[get] Wait response timeout: {}ms. Key: `{}`. Host: `{}`.", timeout, key, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.GET, key);
            return null;
        } catch (IllegalStateException e) {
            LOG.error("[get] MemcachedChannel has been closed. Key: `{}`. Host: `{}`.", key, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.GET, key);
            return null;
        } catch (Exception e) {
            LOG.error("[get] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Host: `" + host + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.GET, key, e.getMessage());
            return null;
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.GET, key, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keySet) {
        Map<String, T> result = new HashMap<>();
        if (keySet == null || keySet.isEmpty()) {
            return result;
        }
        long startTime = System.nanoTime();
        try {
            if (!memcachedChannel.isActive()) {
                LOG.error("[multi-get] Inactive channel. Key set: `{}`. Host: `{}`.", keySet, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onClosed(this, OperationType.MULTI_GET, joinKeyCollection(keySet));
                return result;
            }
            List<byte[]> keyList = new ArrayList<>();
            for (String key : keySet) {
                if (key == null || key.isEmpty()) {
                    executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                    LOG.error("[multi-get] Key could not be empty. Key: `{}`. Key set: `{}`. Host: `{}`.", key, keySet, host);
                    continue;
                }
                byte[] keyBytes = key.getBytes(CHARSET_UTF8);
                if (keyBytes.length > MAX_KEY_LENGTH) {
                    executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                    LOG.error("[multi-get] Key is too large. Key length could not greater than {}. Key: `{}`. Key set: `{}`. Host: `{}`.",
                            MAX_KEY_LENGTH, key, keySet, host);
                    continue;
                }
                keyList.add(keyBytes);
            }
            if (keyList.size() < keySet.size()) {
                clientListener.onInvalidKey(this, OperationType.MULTI_GET, joinKeyCollection(keySet));
            }
            if (!keyList.isEmpty()) {
                List<String> notFoundKeyList = new ArrayList<>();
                MultiGetCommand multiGetCommand = new MultiGetCommand(keyList);
                List<ResponsePacket> responsePacketList = memcachedChannel.send(multiGetCommand, timeout);

                for (ResponsePacket responsePacket : responsePacketList) {
                    String key = new String(responsePacket.getBody(), responsePacket.getExtrasLength(), responsePacket.getKeyLength(), CHARSET_UTF8);
                    if (responsePacket.isSuccess()) {
                        try {
                            T value = transcoder.decode(responsePacket.getBody(), 0,
                                    responsePacket.getExtrasLength() + responsePacket.getKeyLength(),
                                    responsePacket.getValueLength());
                            result.put(key, value);
                        } catch (Exception e) {
                            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                            clientListener.onError(this, OperationType.MULTI_GET, key, e.getMessage());
                            LOG.error("[multi-get] Decode java object failed. Key: `" + key + "`. Key set: `" + keySet
                                    + "`. Host: `" + host + "`.", e);
                        }
                    } else {
                        if (responsePacket.isKeyNotFound()) {
                            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_KEY_NOT_FOUND);
                            notFoundKeyList.add(key);
                            LOG.info("[multi-get] Key not found. Key: `{}`. Key set: `{}`. Host: `{}`", key, keySet, host);
                        } else {
                            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                            clientListener.onError(this, OperationType.MULTI_GET, key, responsePacket.getErrorMessage());
                            LOG.error("[multi-get] Memcached error: `{}`. Key: `{}`. Key set: `{}`. Host: `{}`.",
                                    responsePacket.getErrorMessage(), key, keySet, host);
                        }
                    }
                }

                if (!notFoundKeyList.isEmpty()) {
                    LOG.info("[multi-get] Miss `{}` keys. Hit keys: `{}`. Miss keys: `{}`. Key set: `{}`. Host: `{}`.",
                            notFoundKeyList.size(), result.keySet(), notFoundKeyList, keySet, host);
                    if (clientListener.hasClientListener()) {
                        clientListener.onKeyNotFound(this, OperationType.MULTI_GET, joinKeyCollection(notFoundKeyList));
                    }
                }
                LOG.debug("[multi-get] Key set: `{}`. Host: `{}`. Result: `{}`", keySet, host, result);
            }
            return result;
        } catch (TimeoutException e) {
            LOG.error("[multi-get] Wait response timeout: {}ms. Key set: `{}`. Host: `{}`.", timeout, keySet, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.MULTI_GET, joinKeyCollection(keySet));
            return result;
        } catch (IllegalStateException e) {
            LOG.error("[multi-get] MemcachedChannel has been closed. Key set: `{}`. Host: `{}`.", keySet, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.MULTI_GET, joinKeyCollection(keySet));
            return result;
        } catch (Exception e) {
            LOG.error("[multi-get] Unexpected error: `" + e.getMessage() + "`. Key set: `" + keySet + "`. Host: `" + host + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.MULTI_GET, joinKeyCollection(keySet), e.getMessage());
            return result;
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.MULTI_GET, joinKeyCollection(keySet), executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    /**
     * 将 Key 集合合并成一个字符串输出
     *
     * @param keys Key 集合
     * @return Key 集合合并后的字符串
     */
    private String joinKeyCollection(Collection<String> keys) {
        if (keys == null)
            return "null";
        if (keys.isEmpty())
            return "[]";
        StringBuilder buffer = new StringBuilder("[");
        for (String key : keys) {
            buffer.append(key).append(",");
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("]");
        return buffer.toString();
    }

    @Override
    public boolean add(String key, Object value) {
        return add(key, value, 0);
    }

    @Override
    public boolean add(String key, Object value, int expiry) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[add] Key could not be empty. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.ADD, key);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[add] Key is too large. Key length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.ADD, key);
                return false;
            }
            if (value == null) {
                LOG.error("[add] Value could not be null. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, "null", expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.ADD, key);
                return false;
            }
            if (!(value instanceof Serializable)) {
                LOG.error("[add] Value is not serializable. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.ADD, key);
                return false;
            }
            if (expiry < 0) {
                LOG.error("[add] Expiry could not less than 0. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidExpiry(this, OperationType.ADD, key);
                return false;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[add] Inactive channel. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onClosed(this, OperationType.ADD, key);
                return false;
            }
            byte[][] encodedBytes = transcoder.encode(value);
            if (encodedBytes[1].length > MAX_VALUE_LENGTH) {
                LOG.error("[add] Value is too large. Value length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_VALUE_LENGTH, key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.ADD, key);
                return false;
            }
            AddCommand addCommand = new AddCommand(keyBytes, encodedBytes[1], expiry, encodedBytes[0]);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(addCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    LOG.debug("[add] Success. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                            key, value, expiry, host);
                    return true;
                } else {
                    LOG.error("[add] Memcached error: `{}`. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                            responsePacket.getErrorMessage(), key, value, expiry, host);
                    executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                    clientListener.onError(this, OperationType.ADD, key, responsePacket.getErrorMessage());
                    return false;
                }
            } else {
                LOG.error("[add] Empty response. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`",
                        key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onError(this, OperationType.ADD, key, NO_RESPONSE_PACKET_MESSAGE);
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[add] Wait response timeout: {}ms. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    timeout, key, value, expiry, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.ADD, key);
            return false;
        } catch (IllegalStateException e) {
            LOG.error("[add] MemcachedChannel has been closed. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    key, value, expiry, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.ADD, key);
            return false;
        } catch (Exception e) {
            LOG.error("[add] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Value: `" + value
                    + "`. Expiry: `" + expiry + "`. Host: `" + host + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.ADD, key, e.getMessage());
            return false;
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.ADD, key, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @Override
    public boolean set(String key, Object value) {
        return set(key, value, 0);
    }

    @Override
    public boolean set(String key, Object value, int expiry) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[set] Key could not be empty. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.SET, key);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[set] Key is too large. Key length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.SET, key);
                return false;
            }
            if (value == null) {
                LOG.error("[set] Value could not be null. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, "null", expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.SET, key);
                return false;
            }
            if (!(value instanceof Serializable)) {
                LOG.error("[set] Value is not serializable. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.SET, key);
                return false;
            }
            if (expiry < 0) {
                LOG.error("[set] Expiry could not less than 0. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidExpiry(this, OperationType.SET, key);
                return false;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[set] Inactive channel. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onClosed(this, OperationType.SET, key);
                return false;
            }
            byte[][] encodedBytes = transcoder.encode(value);
            if (encodedBytes[1].length > MAX_VALUE_LENGTH) {
                LOG.error("[set] Value is too large. Value length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_VALUE_LENGTH, key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.SET, key);
                return false;
            }
            SetCommand setCommand = new SetCommand(keyBytes, encodedBytes[1], expiry, encodedBytes[0]);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(setCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    LOG.debug("[set] Success. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                            key, value, expiry, host);
                    return true;
                } else {
                    LOG.error("[set] Memcached error: `{}`. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                            responsePacket.getErrorMessage(), key, value, expiry, host);
                    executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                    clientListener.onError(this, OperationType.SET, key, responsePacket.getErrorMessage());
                    return false;
                }
            } else {
                LOG.error("[set] Empty response. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`",
                        key, value, expiry, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onError(this, OperationType.SET, key, NO_RESPONSE_PACKET_MESSAGE);
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[set] Wait response timeout: {}ms. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    timeout, key, value, expiry, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.SET, key);
            return false;
        } catch (IllegalStateException e) {
            LOG.error("[set] MemcachedChannel has been closed. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    key, value, expiry, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.SET, key);
            return false;
        } catch (Exception e) {
            LOG.error("[set] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Value: `" + value
                    + "`. Expiry: `" + expiry + "`. Host: `" + host + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.SET, key, e.getMessage());
            return false;
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.SET, key, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @Override
    public boolean delete(String key) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[delete] Key could not be empty. Key: `{}`. Host: `{}`.", key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.DELETE, key);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[delete] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.DELETE, key);
                return false;
            }
            DeleteCommand deleteCommand = new DeleteCommand(keyBytes);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(deleteCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    LOG.debug("[delete] Success. Key: `{}`. Host: `{}`.", key, host);
                    return true;
                } else {
                    if (responsePacket.isKeyNotFound()) {
                        LOG.info("[delete] Key not found. Key: `{}`. Host: `{}`", key, host);
                        clientListener.onKeyNotFound(this, OperationType.DELETE, key);
                    } else {
                        LOG.error("[delete] Memcached error: `{}`. Key: `{}`. Host: `{}`.", responsePacket.getErrorMessage(), key, host);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                        clientListener.onError(this, OperationType.DELETE, key, responsePacket.getErrorMessage());
                    }
                    return false;
                }
            } else {
                LOG.error("[delete] Empty response. Key: `{}`. Host: `{}`", key, host);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onError(this, OperationType.DELETE, key, NO_RESPONSE_PACKET_MESSAGE);
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[delete] Wait response timeout: {}ms. Key: `{}`. Host: `{}`.",
                    timeout, key, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.DELETE, key);
            return false;
        } catch (IllegalStateException e) {
            LOG.error("[delete] MemcachedChannel has been closed. Key: `{}`. Host: `{}`.", key, host);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.DELETE, key);
            return false;
        } catch (Exception e) {
            LOG.error("[delete] Unexpected error: `" + e.getMessage() + "`. Key: `" + key
                    + "`. Host: `" + host + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.DELETE, key, e.getMessage());
            return false;
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.DELETE, key, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @Override
    public long addAndGet(String key, long delta, long initialValue, int expiry) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[AddAndGet] Key could not be empty. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                        key, host, delta, initialValue, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.ADD_AND_GET, key);
                return 0;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[AddAndGet] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                        MAX_KEY_LENGTH, key, host, delta, initialValue, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.ADD_AND_GET, key);
                return 0;
            }
            if (initialValue < 0) {
                LOG.error("[AddAndGet] InitialValue could not less than 0. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                        key, host, delta, initialValue, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidValue(this, OperationType.ADD_AND_GET, key);
                return 0;
            }
            if (expiry < 0) {
                LOG.error("[AddAndGet] Expiry could not less than 0. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                        key, host, delta, initialValue, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidExpiry(this, OperationType.ADD_AND_GET, key);
                return 0;
            }
            Command incrOrDecrCommand;
            if (delta >= 0) {
                incrOrDecrCommand = new IncrementCommand(keyBytes, delta, initialValue, expiry);
            } else {
                incrOrDecrCommand = new DecrementCommand(keyBytes, Math.abs(delta), initialValue, expiry);
            }
            List<ResponsePacket> responsePacketList = memcachedChannel.send(incrOrDecrCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    if (responsePacket.getValueLength() == 8) {
                        long result = ByteUtil.eightByteArrayToLong(responsePacket.getBody(), responsePacket.getExtrasLength() + responsePacket.getKeyLength());
                        LOG.debug("[AddAndGet] Success. Key: `{}`. Result: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                                key, result, host, delta, initialValue, expiry);
                        return result;
                    } else {//should not happen
                        LOG.error("[AddAndGet] Invalid response value length: `{}`. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                                responsePacket.getValueLength(), key, host, delta, initialValue, expiry);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                        clientListener.onError(this, OperationType.ADD_AND_GET, key, "invalid response value length");
                        return 0;
                    }
                } else {
                    if (responsePacket.isKeyNotFound()) {
                        LOG.info("[AddAndGet] Key not found. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                                key, host, delta, initialValue, expiry);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_KEY_NOT_FOUND);
                        clientListener.onKeyNotFound(this, OperationType.ADD_AND_GET, key);
                    } else {
                        LOG.error("[AddAndGet] Memcached error: `{}`. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                                responsePacket.getErrorMessage(), key, host, delta, initialValue, expiry);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                        clientListener.onError(this, OperationType.ADD_AND_GET, key, responsePacket.getErrorMessage());
                    }
                    return 0;
                }
            } else {
                LOG.error("[AddAndGet] Empty response. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                        key, host, delta, initialValue, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onError(this, OperationType.ADD_AND_GET, key, NO_RESPONSE_PACKET_MESSAGE);
                return 0;
            }
        } catch (TimeoutException e) {
            LOG.error("[AddAndGet] Wait response timeout: `{} ms`. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                    timeout, key, host, delta, initialValue, expiry);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.ADD_AND_GET, key);
            return 0;
        } catch (IllegalStateException e) {
            LOG.error("[AddAndGet] MemcachedChannel has been closed. Key: `{}`. Host: `{}`. Delta: `{}`. InitialValue: `{}`. Expiry: `{}`.",
                    key, host, delta, initialValue, expiry);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.ADD_AND_GET, key);
            return 0;
        } catch (Exception e) {
            LOG.error("[AddAndGet] Unexpected error: `" + e.getMessage() + "`. Key: `" + key
                    + "`. Host: `" + host + "`. Delta: `" + delta + "`. InitialValue: `" + initialValue + "`. Expiry: `"
                    + expiry + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.ADD_AND_GET, key, e.getMessage());
            return 0;
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.ADD_AND_GET, key, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @Override
    public void touch(String key, int expiry) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[Touch] Key could not be empty. Key: `{}`. Host: `{}`. Expiry: `{}`.", key, host, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.TOUCH, key);
                return;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[Touch] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`. Expiry: `{}`.",
                        MAX_KEY_LENGTH, key, host, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidKey(this, OperationType.TOUCH, key);
                return;
            }
            if (expiry < 0) {
                LOG.error("[Touch] Expiry could not less than 0. Key: `{}`. Host: `{}`. Expiry: `{}`.", key, host, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onInvalidExpiry(this, OperationType.TOUCH, key);
                return;
            }
            Command touchCommand = new TouchCommand(keyBytes, expiry);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(touchCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    LOG.debug("[Touch] Success. Key: `{}`. Host: `{}`. Expiry: `{}`.", key, host, expiry);
                } else {
                    if (responsePacket.isKeyNotFound()) {
                        LOG.info("[Touch] Key not found. Key: `{}`. Host: `{}`. Expiry: `{}`.", key, host, expiry);
                        clientListener.onKeyNotFound(this, OperationType.TOUCH, key);
                    } else {
                        LOG.error("[Touch] Memcached error: `{}`. Key: `{}`. Host: `{}`. Expiry: `{}`.",
                                responsePacket.getErrorMessage(), key, host, expiry);
                        executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                        clientListener.onError(this, OperationType.TOUCH, key, responsePacket.getErrorMessage());
                    }
                }
            } else {
                LOG.error("[Touch] Empty response. Key: `{}`. Host: `{}`. Expiry: `{}`.", key, host, expiry);
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
                clientListener.onError(this, OperationType.TOUCH, key, NO_RESPONSE_PACKET_MESSAGE);
            }
        } catch (TimeoutException e) {
            LOG.error("[Touch] Wait response timeout: `{} ms`. Key: `{}`. Host: `{}`. Expiry: `{}`.", timeout, key, host, expiry);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_TIMEOUT);
            clientListener.onTimeout(this, OperationType.TOUCH, key);
        } catch (IllegalStateException e) {
            LOG.error("[Touch] MemcachedChannel has been closed. Key: `{}`. Host: `{}`. Expiry: `{}`.", key, host, expiry);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onClosed(this, OperationType.TOUCH, key);
        } catch (Exception e) {
            LOG.error("[Touch] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Host: `" + host
                    + "`. Expiry: `" + expiry + "`.", e);
            executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_MEMCACHED_ERROR);
            clientListener.onError(this, OperationType.TOUCH, key, e.getMessage());
        } finally {
            long executedNanoTime = System.nanoTime() - startTime;
            if (executedNanoTime > NaiveMemcachedClientListener.SLOW_EXECUTION_THRESHOLD) {
                executionMonitor.onError(ExecutionMonitorFactory.ERROR_CODE_SLOW_EXECUTION);
                clientListener.onSlowExecution(this, OperationType.TOUCH, key, executedNanoTime);
            }
            executionMonitor.onExecuted(startTime);
        }
    }

    @Override
    public boolean isActive() {
        return memcachedChannel.isActive();
    }

    @Override
    public void close() {
        memcachedChannel.close();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        return "DirectMemcachedClient{" +
                "host='" + host + '\'' +
                ", timeout=" + timeout +
                '}';
    }

    private static class ClientListenerWrapper implements NaiveMemcachedClientListener {

        private final NaiveMemcachedClientListener clientListener;

        private final Logger logger;

        private ClientListenerWrapper(NaiveMemcachedClientListener clientListener) {
            this.clientListener = clientListener;
            this.logger = DirectMemcachedClient.LOG;
        }

        private boolean hasClientListener() {
            return clientListener != null;
        }

        @Override
        public void onInvalidKey(NaiveMemcachedClient client, OperationType operationType, String key) {
            if (hasClientListener()) {
                try {
                    clientListener.onInvalidKey(client, operationType, key);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onInvalidKey() failed. Client: `" + client + "`. Operation: `" + operationType
                        + "`. key: `" + key + "`.", e);
                }
            }
        }

        @Override
        public void onInvalidValue(NaiveMemcachedClient client, OperationType operationType, String key) {
            if (hasClientListener()) {
                try {
                    clientListener.onInvalidValue(client, operationType, key);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onInvalidValue() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`.", e);
                }
            }
        }

        @Override
        public void onInvalidExpiry(NaiveMemcachedClient client, OperationType operationType, String key) {
            if (hasClientListener()) {
                try {
                    clientListener.onInvalidExpiry(client, operationType, key);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onInvalidExpiry() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`.", e);
                }
            }
        }

        @Override
        public void onClosed(NaiveMemcachedClient client, OperationType operationType, String key) {
            if (hasClientListener()) {
                try {
                    clientListener.onClosed(client, operationType, key);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onClosed() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`.", e);
                }
            }
        }

        @Override
        public void onKeyNotFound(NaiveMemcachedClient client, OperationType operationType, String key) {
            if (hasClientListener()) {
                try {
                    clientListener.onKeyNotFound(client, operationType, key);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onKeyNotFound() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`.", e);
                }
            }
        }

        @Override
        public void onTimeout(NaiveMemcachedClient client, OperationType operationType, String key) {
            if (hasClientListener()) {
                try {
                    clientListener.onTimeout(client, operationType, key);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onTimeout() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`.", e);
                }
            }
        }

        @Override
        public void onError(NaiveMemcachedClient client, OperationType operationType, String key, String errorMessage) {
            if (hasClientListener()) {
                try {
                    clientListener.onError(client, operationType, key, errorMessage);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onError() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`. ErrorMessage: `" + errorMessage + "`.", e);
                }
            }
        }

        @Override
        public void onSlowExecution(NaiveMemcachedClient client, OperationType operationType, String key, long executedNanoTime) {
            if (hasClientListener()) {
                try {
                    clientListener.onSlowExecution(client, operationType, key, executedNanoTime);
                } catch (Exception e) {
                    logger.error("Call NaiveMemcachedClientListener#onSlowExecution() failed. Client: `" + client + "`. Operation: `" + operationType
                            + "`. key: `" + key + "`. ExecutedNanoTime: `" + executedNanoTime + "`.", e);
                }
            }
        }
    }

}
