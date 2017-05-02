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

import com.heimuheimu.naivecache.memcached.binary.channel.MemcachedChannel;
import com.heimuheimu.naivecache.memcached.binary.command.GetCommand;
import com.heimuheimu.naivecache.memcached.binary.command.SetCommand;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.binary.transcoder.SimpleTranscoder;
import com.heimuheimu.naivecache.memcached.binary.transcoder.Transcoder;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author heimuheimu
 * @ThreadSafe
 */
public class MemcachedClient {

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedClient.class);

    private static final Charset CHARSET_UTF8 = Charset.forName("utf-8");

    private final int timeout = 1000;

    private final MemcachedChannel memcachedChannel;

    private final Transcoder transcoder = new SimpleTranscoder();

    public MemcachedClient(MemcachedChannel memcachedChannel) {
        this.memcachedChannel = memcachedChannel;
    }

    public Object get(String key) {
        if (key == null || key.isEmpty() || key.length() > 255) {
            LOG.error("[get] Key is empty or too large. Key: {}", key);
            return null;
        }
        try {
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            GetCommand getCommand = new GetCommand(keyBytes);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(getCommand, timeout);
            ResponsePacket responsePacket = responsePacketList.get(0);
            if (responsePacket.isSuccess()) {
                return transcoder.decode(responsePacket.getBody(), 0,
                        responsePacket.getExtrasLength() + responsePacket.getKeyLength(),
                        responsePacket.getValueLength());
            } else {
                byte[] header = responsePacket.getHeader();
                if (header[6] != 0 && header[7] != 1) { //如果不为Not Found错误，则打印日志
                    LOG.error("[get] {}. Key: {}", responsePacket.getErrorMessage(), key);
                }
                return null;
            }
        } catch(TimeoutException e) {
            LOG.error("[get] Wait response timeout: {}ms. Key: {}", timeout, key);
            return null;
        } catch (Exception e) {
            LOG.error("[get] Unexpected error: " + e.getMessage() + ". Key: " + key, e);
            return null;
        }
    }

    public void set(String key, Object value, int expiry) {
        if (key == null || key.isEmpty() || key.length() > 255) {
            LOG.error("[set] Key is empty or too large. Key: {}", key);
            return;
        }
        if (value == null) {
            LOG.error("[set] Value could not be null. Key: {}", key);
            return;
        }
        if (expiry < 0) {
            LOG.error("[set] Expiry could not less than 0. Key: {}. Expiry: {}", key, expiry);
            return;
        }
        try {
            byte[] keyBytes = key.getBytes(CHARSET_UTF8);
            byte[][] encodedBytes = transcoder.encode(value);
            SetCommand setCommand = new SetCommand(keyBytes, encodedBytes[1], expiry, encodedBytes[0]);
            List<ResponsePacket> responsePacketList = memcachedChannel.send(setCommand, timeout);
            ResponsePacket responsePacket = responsePacketList.get(0);
            if (!responsePacket.isSuccess()) {
                LOG.error("[set] {}. Key: {}", responsePacket.getErrorMessage(), key);
            }
        } catch(TimeoutException e) {
            LOG.error("[set] Wait response timeout: {}ms. Key: {}", timeout, key);
        } catch (Exception e) {
            LOG.error("[set] Unexpected error: " + e.getMessage() + ". Key: " + key, e);
        }
    }

}
