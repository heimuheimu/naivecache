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

package com.heimuheimu.naivecache.monitor.compress;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 压缩统计信息
 *
 * @author heimuheimu
 */
public class CompressionInfo {

    /**
     * 未压缩前字节数
     */
    private final AtomicLong preCompressed = new AtomicLong();

    /**
     * 压缩后的字节数
     */
    private final AtomicLong compressed = new AtomicLong();

    /**
     * 解压缩前的字节数
     */
    private final AtomicLong preDecompressed = new AtomicLong();

    /**
     * 解压缩后的字节数
     */
    private final AtomicLong decompressed = new AtomicLong();

    /**
     * 增加一个压缩统计信息
     *
     * @param preCompressed 未压缩前字节数
     * @param compressed 压缩后的字节数
     */
    public void addCompress(long preCompressed, long compressed) {
        this.preCompressed.addAndGet(preCompressed);
        this.compressed.addAndGet(compressed);
    }

    /**
     * 增加一个解压缩统计信息
     *
     * @param preDecompressed 解压缩前的字节数
     * @param decompressed 解压缩后的字节数
     */
    public void addDecompress(long preDecompressed, long decompressed) {
        this.preDecompressed.addAndGet(preDecompressed);
        this.decompressed.addAndGet(decompressed);
    }

    public AtomicLong getPreCompressed() {
        return preCompressed;
    }

    public AtomicLong getCompressed() {
        return compressed;
    }

    public AtomicLong getPreDecompressed() {
        return preDecompressed;
    }

    public AtomicLong getDecompressed() {
        return decompressed;
    }

    @Override
    public String toString() {
        return "CompressionInfo{" +
                "preCompressed=" + preCompressed +
                ", compressed=" + compressed +
                ", preDecompressed=" + preDecompressed +
                ", decompressed=" + decompressed +
                '}';
    }
}
