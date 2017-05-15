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
import com.heimuheimu.naivecache.memcached.binary.channel.MemcachedChannel;
import com.heimuheimu.naivecache.memcached.binary.command.DeleteCommand;
import com.heimuheimu.naivecache.memcached.binary.command.GetCommand;
import com.heimuheimu.naivecache.memcached.binary.command.MultiGetCommand;
import com.heimuheimu.naivecache.memcached.binary.command.SetCommand;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;
import com.heimuheimu.naivecache.memcached.monitor.MemcachedMonitor;
import com.heimuheimu.naivecache.memcached.monitor.OperationResult;
import com.heimuheimu.naivecache.memcached.monitor.OperationType;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import com.heimuheimu.naivecache.transcoder.SimpleTranscoder;
import com.heimuheimu.naivecache.transcoder.Transcoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Memcached 直连客户端，基于二进制协议进行通信
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class DirectMemcachedClient implements NaiveMemcachedClient {

    private static final Logger LOG = LoggerFactory.getLogger(DirectMemcachedClient.class);

    private static final Charset CHARSET_UTF8 = Charset.forName("utf-8");

    private final String host;

    private final int timeout;

    private final MemcachedChannel memcachedChannel;

    private final Transcoder transcoder;

    /**
     * 创建一个 Memcached 直连客户端
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @param configuration Socket配置信息，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @param timeout Memcached 操作超时时间，单位：毫秒，不能小于等于0
     * @param compressionThreshold 最小压缩字节数，当 Value 字节数小于或等于该值，不进行压缩，不能小于等于0
     */
    public DirectMemcachedClient(String host, SocketConfiguration configuration,
                                 int timeout, int compressionThreshold) {
        this.memcachedChannel = new MemcachedChannel(host, configuration);
        this.memcachedChannel.init();
        this.host = host;
        this.timeout = timeout;
        this.transcoder = new SimpleTranscoder(compressionThreshold);
    }

    @Override
    public <T> T get(String key) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[get] Key could not be empty. Key: `{}`. Host: `{}`.", key, host);
                MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
                return null;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[get] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, host);
                MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
                return null;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[get] Inactive channel. Key: `{}`. Host: `{}`.", key, host);
                MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
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
                    MemcachedMonitor.add(host, OperationType.GET, OperationResult.SUCCESS, startTime);
                    return value;
                } else {
                    if (responsePacket.isKeyNotFound()) {
                        LOG.info("[get] Key not found. Key: `{}`. Host: `{}`", key, host);
                        MemcachedMonitor.add(host, OperationType.GET, OperationResult.MISS, startTime);
                    } else {
                        LOG.error("[get] Memcached error: `{}`. Key: `{}`. Host: `{}`.", responsePacket.getErrorMessage(), key, host);
                        MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
                    }
                    return null;
                }
            } else {
                LOG.error("[get] Empty response. Key: `{}`. Host: `{}`", key, host);
                MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
                return null;
            }
        } catch(TimeoutException e) {
            LOG.error("[get] Wait response timeout: {}ms. Key: `{}`. Host: `{}`.", timeout, key, host);
            MemcachedMonitor.add(host, OperationType.GET, OperationResult.TIMEOUT, startTime);
            return null;
        } catch (IllegalStateException e) {
            LOG.error("[get] MemcachedChannel has benn closed. Key: `{}`. Host: `{}`.", key, host);
            MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
            return null;
        } catch (Exception e) {
            LOG.error("[get] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Host: `" + host + "`.", e);
            MemcachedMonitor.add(host, OperationType.GET, OperationResult.ERROR, startTime);
            return null;
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keySet) {
        long startTime = System.nanoTime();
        Map<String, T> result = new HashMap<>();
        try {
            if (keySet == null || keySet.isEmpty()) {
                LOG.error("[multi-get] Key set could not be empty. Key set: `{}`. Host: `{}`.", keySet, host);
                MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.ERROR, startTime);
                return result;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[multi-get] Inactive channel. Key set: `{}`. Host: `{}`.", keySet, host);
                MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.ERROR, startTime);
                return result;
            }
            List<byte[]> keyList = new ArrayList<>();
            for (String key : keySet) {
                if (key == null || key.isEmpty()) {
                    LOG.error("[multi-get] Key could not be empty. Key: `{}`. Key set: `{}`. Host: `{}`.", key, keySet, host);
                    continue;
                }
                byte[] keyBytes = key.getBytes(CHARSET_UTF8);
                if (keyBytes.length > MAX_KEY_LENGTH) {
                    LOG.error("[multi-get] Key is too large. Key length could not greater than {}. Key: `{}`. Key set: `{}`. Host: `{}`.",
                            MAX_KEY_LENGTH, key, keySet, host);
                    continue;
                }
                keyList.add(keyBytes);
            }
            if (keyList.size() < keySet.size()) {
                MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.ERROR, startTime);
            }
            if (!keyList.isEmpty()) {
                MultiGetCommand multiGetCommand = new MultiGetCommand(keyList);
                List<ResponsePacket> responsePacketList = memcachedChannel.send(multiGetCommand, timeout);
                for (ResponsePacket responsePacket : responsePacketList) {
                    String key = new String(responsePacket.getBody(), responsePacket.getExtrasLength(), responsePacket.getKeyLength(), CHARSET_UTF8);
                    if (responsePacket.isSuccess()) {
                        T value = transcoder.decode(responsePacket.getBody(), 0,
                                responsePacket.getExtrasLength() + responsePacket.getKeyLength(),
                                responsePacket.getValueLength());
                        result.put(key, value);
                    } else {
                        if (responsePacket.isKeyNotFound()) {
                            LOG.info("[multi-get] Key not found. Key: `{}`. Key set: `{}`. Host: `{}`", key, keySet, host);
                        } else {
                            LOG.error("[multi-get] Memcached error: `{}`. Key: `{}`. Key set: `{}`. Host: `{}`.",
                                    responsePacket.getErrorMessage(), key, keySet, host);
                        }
                    }
                }
                LOG.debug("[multi-get] Key set: `{}`. Host: `{}`. Result: `{}`", keySet, host, result);
                if (result.size() < keySet.size()) {
                    LOG.info("[multi-get] Miss `{}` keys. Hit keys: `{}`. Key set: `{}`. Host: `{}`.",
                            keySet.size() - result.size(), result.keySet(), keySet, host);
                    MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.MISS, startTime);
                } else {
                    MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.SUCCESS, startTime);
                }
            }
            return result;
        } catch (TimeoutException e) {
            LOG.error("[multi-get] Wait response timeout: {}ms. Key set: `{}`. Host: `{}`.", timeout, keySet, host);
            MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.TIMEOUT, startTime);
            return result;
        } catch (IllegalStateException e) {
            LOG.error("[multi-get] MemcachedChannel has benn closed. Key set: `{}`. Host: `{}`.", keySet, host);
            MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.ERROR, startTime);
            return result;
        } catch (Exception e) {
            LOG.error("[multi-get] Unexpected error: `" + e.getMessage() + "`. Key set: `" + keySet + "`. Host: `" + host + "`.", e);
            MemcachedMonitor.add(host, OperationType.MULTI_GET, OperationResult.ERROR, startTime);
            return result;
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
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[set] Key is too large. Key length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, value, expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            if (value == null) {
                LOG.error("[set] Value could not be null. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, "null", expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            if (!(value instanceof Serializable)) {
                LOG.error("[set] Value is not serializable. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            if (expiry < 0) {
                LOG.error("[set] Expiry could not less than 0. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[set] Inactive channel. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            byte[][] encodedBytes = transcoder.encode(value);
            if (encodedBytes[1].length > MAX_VALUE_LENGTH) {
                LOG.error("[set] Value is too large. Value length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_VALUE_LENGTH, key, value, expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
            SetCommand setCommand = new SetCommand(keyBytes, encodedBytes[1], expiry, encodedBytes[0]);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(setCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    LOG.debug("[set] Success. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                            key, value, expiry, host);
                    MemcachedMonitor.add(host, OperationType.SET, OperationResult.SUCCESS, startTime);
                    return true;
                } else {
                    LOG.error("[set] Memcached error: `{}`. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                            responsePacket.getErrorMessage(), key, value, expiry, host);
                    MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                    return false;
                }
            } else {
                LOG.error("[set] Empty response. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`",
                        key, value, expiry, host);
                MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[set] Wait response timeout: {}ms. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    timeout, key, value, expiry, host);
            MemcachedMonitor.add(host, OperationType.SET, OperationResult.TIMEOUT, startTime);
            return false;
        } catch (IllegalStateException e) {
            LOG.error("[set] MemcachedChannel has benn closed. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    key, value, expiry, host);
            MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
            return false;
        } catch (Exception e) {
            LOG.error("[set] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Value: `" + value
                    + "`. Expiry: `" + expiry + "`. Host: `" + host + "`.", e);
            MemcachedMonitor.add(host, OperationType.SET, OperationResult.ERROR, startTime);
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        long startTime = System.nanoTime();
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[delete] Key could not be empty. Key: `{}`. Host: `{}`.", key, host);
                MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.ERROR, startTime);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            if (keyBytes.length > MAX_KEY_LENGTH) {
                LOG.error("[delete] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, host);
                MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.ERROR, startTime);
                return false;
            }
            DeleteCommand deleteCommand = new DeleteCommand(keyBytes);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(deleteCommand, timeout);
            if (!responsePacketList.isEmpty()) {
                ResponsePacket responsePacket = responsePacketList.get(0);
                if (responsePacket.isSuccess()) {
                    LOG.debug("[delete] Success. Key: `{}`. Host: `{}`.", key, host);
                    MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.SUCCESS, startTime);
                    return true;
                } else {
                    if (responsePacket.isKeyNotFound()) {
                        LOG.info("[delete] Key not found. Key: `{}`. Host: `{}`", key, host);
                        MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.MISS, startTime);
                    } else {
                        LOG.error("[delete] Memcached error: `{}`. Key: `{}`. Host: `{}`.", responsePacket.getErrorMessage(), key, host);
                        MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.ERROR, startTime);
                    }
                    return false;
                }
            } else {
                LOG.error("[delete] Empty response. Key: `{}`. Host: `{}`", key, host);
                MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.ERROR, startTime);
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[delete] Wait response timeout: {}ms. Key: `{}`. Host: `{}`.",
                    timeout, key, host);
            MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.TIMEOUT, startTime);
            return false;
        } catch (IllegalStateException e) {
            LOG.error("[delete] MemcachedChannel has benn closed. Key: `{}`. Host: `{}`.", key, host);
            MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.ERROR, startTime);
            return false;
        } catch (Exception e) {
            LOG.error("[delete] Unexpected error: `" + e.getMessage() + "`. Key: `" + key
                    + "`. Host: `" + host + "`.", e);
            MemcachedMonitor.add(host, OperationType.DELETE, OperationResult.ERROR, startTime);
            return false;
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

}
