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

package com.heimuheimu.naivecache.transcoder;

import com.heimuheimu.naivecache.memcached.exception.MemcachedException;
import com.heimuheimu.naivecache.memcached.monitor.CompressionMonitorFactory;
import com.heimuheimu.naivecache.transcoder.compression.LZFUtil;
import com.heimuheimu.naivemonitor.monitor.CompressionMonitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 使用 Java 序列化实现的 Java 对象与 Memcached 二进制协议存储的字节数组转换器
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class SimpleTranscoder implements Transcoder {

    private static final byte TRANSCODER_VERSION_BYTE = 1;

    private static final byte LZF_COMPRESSION_BYTE = 1;

    /**
     * 当 Value 字节数小于或等于该值，不进行压缩
     */
    private final int compressionThreshold;

    /**
     * 压缩信息监控器
     */
    private final CompressionMonitor compressionMonitor;

    public SimpleTranscoder(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
        this.compressionMonitor = CompressionMonitorFactory.get();
    }

    @Override
    public byte[][] encode(Object value) throws Exception {

        byte[][] result = new byte[2][0];
        byte[] flags = new byte[4];
        flags[0] = TRANSCODER_VERSION_BYTE;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(value);
        byte[] valueBytes = bos.toByteArray();
        oos.close();
        int preCompressedLength = valueBytes.length;
        if (valueBytes.length > compressionThreshold) {
            valueBytes = LZFUtil.compress(valueBytes);
            compressionMonitor.onCompressed(preCompressedLength - valueBytes.length);
            flags[1] = LZF_COMPRESSION_BYTE;
        }
        result[0] = flags;
        result[1] = valueBytes;
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] src, int flagsOffset, int valueOffset, int valueLength) throws Exception {
        int flagVersion = src[flagsOffset];
        int compressionByte = src[flagsOffset + 1];
        if (flagVersion == TRANSCODER_VERSION_BYTE) {
            ByteArrayInputStream valueBis;
            if (compressionByte != LZF_COMPRESSION_BYTE) {
                valueBis = new ByteArrayInputStream(src, valueOffset, valueLength);
            } else {
                byte[] value = LZFUtil.decompress(src, valueOffset, valueLength);
                valueBis = new ByteArrayInputStream(value);
            }
            ObjectInputStream ois = new ObjectInputStream(valueBis);
            return (T) ois.readObject();
        } else {
            throw new MemcachedException("Unknown transcoder version: `" + flagVersion + "`");
        }
    }

}
