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
import com.heimuheimu.naivecache.memcached.binary.transcoder.SimpleTranscoder;
import com.heimuheimu.naivecache.memcached.binary.transcoder.Transcoder;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;
import com.heimuheimu.naivecache.net.SocketBuilder;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.Socket;
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

    private final Transcoder transcoder = new SimpleTranscoder();

    public DirectMemcachedClient(String host, SocketConfiguration configuration, int timeout) {
        Socket socket = SocketBuilder.create(host, configuration);
        this.memcachedChannel = new MemcachedChannel(socket);
        this.memcachedChannel.init();
        this.host = host;
        this.timeout = timeout;
    }

    @Override
    public <T> T get(String key) {
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[get] Key could not be empty. Key: `{}`. Host: `{}`.", key, host);
                return null;
            }
            if (key.length() > MAX_KEY_LENGTH) {
                LOG.error("[get] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, host);
                return null;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[get] Inactive channel. Key: `{}`. Host: `{}`.", key, host);
                return null;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            GetCommand getCommand = new GetCommand(keyBytes);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(getCommand, timeout);
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
                } else {
                    LOG.error("[get] Memcached error: `{}`. Key: `{}`. Host: `{}`.", responsePacket.getErrorMessage(), key, host);
                }
                return null;
            }
        } catch(TimeoutException e) {
            LOG.error("[get] Wait response timeout: {}ms. Key: `{}`. Host: `{}`.", timeout, key, host);
            return null;
        } catch (Exception e) {
            LOG.error("[get] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Host: `" + host + "`.", e);
            return null;
        }
    }

    @Override
    public <T> Map<String, T> multiGet(Set<String> keySet) {
        Map<String, T> result = new HashMap<>();
        try {
            if (keySet == null || keySet.isEmpty()) {
                LOG.error("[multi-get] Key set could not be empty. Key set: `{}`. Host: `{}`.", keySet, host);
                return result;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[multi-get] Inactive channel. Key set: `{}`. Host: `{}`.", keySet, host);
                return result;
            }
            List<byte[]> keyList = new ArrayList<>();
            for (String key : keySet) {
                if (key == null || key.isEmpty()) {
                    LOG.error("[multi-get] Key could not be empty. Key: `{}`. Key set: `{}`. Host: `{}`.", key, keySet, host);
                    continue;
                }
                if (key.length() > MAX_KEY_LENGTH) {
                    LOG.error("[multi-get] Key is too large. Key length could not greater than {}. Key: `{}`. Key set: `{}`. Host: `{}`.",
                            MAX_KEY_LENGTH, key, keySet, host);
                    continue;
                }
                keyList.add(key.getBytes(CHARSET_UTF8));
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
                }
            }
            return result;
        } catch (TimeoutException e) {
            LOG.error("[multi-get] Wait response timeout: {}ms. Key set: `{}`. Host: `{}`.", timeout, keySet, host);
            return result;
        } catch (Exception e) {
            LOG.error("[multi-get] Unexpected error: `" + e.getMessage() + "`. Key set: `" + keySet + "`. Host: `" + host + "`.", e);
            return result;
        }
    }

    @Override
    public boolean set(String key, Object value) {
        return set(key, value, 0);
    }

    @Override
    public boolean set(String key, Object value, int expiry) {
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[set] Key could not be empty. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                return false;
            }
            if (key.length() > MAX_KEY_LENGTH) {
                LOG.error("[set] Key is too large. Key length could not greater than {}. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, value, expiry, host);
                return false;
            }
            if (value == null) {
                LOG.error("[set] Value could not be null. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, "null", expiry, host);
                return false;
            }
            if (!(value instanceof Serializable)) {
                LOG.error("[set] Value is not serializable. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                return false;
            }
            if (expiry < 0) {
                LOG.error("[set] Expiry could not less than 0. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                return false;
            }
            if (!memcachedChannel.isActive()) {
                LOG.error("[set] Inactive channel. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.", key, value, expiry, host);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            byte[][] encodedBytes = transcoder.encode(value);
            SetCommand setCommand = new SetCommand(keyBytes, encodedBytes[1], expiry, encodedBytes[0]);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(setCommand, timeout);
            ResponsePacket responsePacket = responsePacketList.get(0);
            if (responsePacket.isSuccess()) {
                LOG.debug("[set] Success. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        key, value, expiry, host);
                return true;
            } else {
                LOG.error("[set] Memcached error: `{}`. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                        responsePacket.getErrorMessage(), key, value, expiry, host);
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[set] Wait response timeout: {}ms. Key: `{}`. Value: `{}`. Expiry: `{}`. Host: `{}`.",
                    timeout, key, value, expiry, host);
            return false;
        } catch (Exception e) {
            LOG.error("[set] Unexpected error: `" + e.getMessage() + "`. Key: `" + key + "`. Value: `" + value
                    + "`. Expiry: `" + expiry + "`. Host: `" + host + "`.", e);
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            if (key == null || key.isEmpty()) {
                LOG.error("[delete] Key could not be empty. Key: `{}`. Host: `{}`.", key, host);
                return false;
            }
            if (key.length() > MAX_KEY_LENGTH) {
                LOG.error("[delete] Key is too large. Key length could not greater than {}. Key: `{}`. Host: `{}`.",
                        MAX_KEY_LENGTH, key, host);
                return false;
            }
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            DeleteCommand deleteCommand = new DeleteCommand(keyBytes);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(deleteCommand, timeout);
            ResponsePacket responsePacket = responsePacketList.get(0);
            if (responsePacket.isSuccess()) {
                LOG.debug("[delete] Success. Key: `{}`. Host: `{}`.", key, host);
                return true;
            } else {
                if (responsePacket.isKeyNotFound()) {
                    LOG.info("[delete] Key not found. Key: `{}`. Host: `{}`", key, host);
                } else {
                    LOG.error("[delete] Memcached error: `{}`. Key: `{}`. Host: `{}`.", responsePacket.getErrorMessage(), key, host);
                }
                return false;
            }
        } catch (TimeoutException e) {
            LOG.error("[delete] Wait response timeout: {}ms. Key: `{}`. Host: `{}`.",
                    timeout, key, host);
            return false;
        } catch (Exception e) {
            LOG.error("[delete] Unexpected error: `" + e.getMessage() + "`. Key: `" + key
                    + "`. Host: `" + host + "`.", e);
            return false;
        }
    }

}
