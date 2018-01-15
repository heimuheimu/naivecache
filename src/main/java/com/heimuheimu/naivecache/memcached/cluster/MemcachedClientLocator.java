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

package com.heimuheimu.naivecache.memcached.cluster;

/**
 * Memcached 客户端定位器，根据 Memcached Key 获取对应的 Memcached 客户端索引。
 *
 * @author heimuheimu
 */
public interface MemcachedClientLocator {

    /**
     * 根据 Memcached Key 获取对应的 Memcached 客户端索引
     *
     * @param key Memcached Key
     * @param clients Memcached 客户端数量
     * @return 该Memcached Key 获取对应的 Memcached 客户端索引
     * @throws IllegalArgumentException 如果 {@code key} 为 {@code null} 或者为空
     * @throws IllegalArgumentException 如果 {@code clients} 小于等于0
     */
    int getIndex(String key, int clients) throws IllegalArgumentException;

}
