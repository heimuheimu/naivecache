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

package com.heimuheimu.naivecache.memcached.cluster.hash;

import com.heimuheimu.naivecache.memcached.cluster.MemcachedClientLocator;

/**
 * 使用一致性 Hash 算法实现的 Memcached 客户端定位器。一致性 Hash 算法定义可参考文档：
 * <p>
 *     <a href="https://en.wikipedia.org/wiki/Consistent_hashing">https://en.wikipedia.org/wiki/Consistent_hashing</a>
 * </p>
 *
 * <p>算法实现使用了 google guava 的一致性哈希算法，更多细节可查看原始实现：</p>
 * <p>
 *     <a href="https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/Hashing.java">
 *     https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/Hashing.java
 *     </a>
 * </p>
 *
 * @author heimuheimu
 */
public class ConsistentHashLocator implements MemcachedClientLocator {

    @Override
    public int getIndex(String key, int clients) throws IllegalArgumentException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key could not be empty: " + key + ". Clients: " + clients);
        }
        if (clients <= 0) {
            throw new IllegalArgumentException("Clients could not equal or less than 0. Key: "
                    + key + ". Clients: " + clients);
        }
        return consistentHash(key.hashCode(), clients);
    }


    private int consistentHash(long input, int buckets) {
        LinearCongruentialGenerator generator = new LinearCongruentialGenerator(input);
        int candidate = 0;
        int next;

        while (true) {
            next = (int) ((candidate + 1) / generator.nextDouble());
            if (next >= 0 && next < buckets) {
                candidate = next;
            } else {
                return candidate;
            }
        }
    }

    private static final class LinearCongruentialGenerator {
        private long state;

        private LinearCongruentialGenerator(long seed) {
            this.state = seed;
        }

        private double nextDouble() {
            state = 2862933555777941757L * state + 1;
            return ((double) ((int) (state >>> 33) + 1)) / (0x1.0p31);
        }
    }
}
