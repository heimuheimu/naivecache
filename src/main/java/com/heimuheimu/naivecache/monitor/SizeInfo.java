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

package com.heimuheimu.naivecache.monitor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 长度统计信息，通常用于统计字节长度
 * <p>大小将以 2 的次方为区间进行统计</p>
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
@SuppressWarnings("unused")
public class SizeInfo {

    private final AtomicLong size = new AtomicLong();

    private final AtomicLong count = new AtomicLong();

    private final long[] sizeLimits;

    private final AtomicLong[] sizeCounts;

    public SizeInfo() {
        sizeLimits = new long[32];
        long limit = 1;
        for (int i = 0; i < 32; i++) {
            limit *= 2;
            sizeLimits[i] = limit;
        }
        sizeCounts = new AtomicLong[33];
        for (int i = 0; i < 33; i++) {
            sizeCounts[i] = new AtomicLong();
        }
    }

    /**
     * 新增一个长度统计
     *
     * @param size 长度
     */
    public void add(long size) {
        this.size.addAndGet(size);
        this.count.incrementAndGet();
        int i = 0;
        for (long sizeLimit : sizeLimits) {
            if (size < sizeLimit) {
                break;
            } else {
                i++;
            }
        }
        sizeCounts[i].incrementAndGet();
    }

    public AtomicLong getSize() {
        return size;
    }

    public AtomicLong getCount() {
        return count;
    }

    public long[] getSizeLimits() {
        return Arrays.copyOf(sizeLimits, sizeLimits.length);
    }

    public AtomicLong[] getSizeCounts() {
        return Arrays.copyOf(sizeCounts, sizeCounts.length);
    }

    @Override
    public String toString() {
        return "SizeInfo{" +
                "size=" + size +
                ", count=" + count +
                ", sizeLimits=" + Arrays.toString(sizeLimits) +
                ", sizeCounts=" + Arrays.toString(sizeCounts) +
                '}';
    }

}
