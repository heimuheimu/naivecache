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

package com.heimuheimu.naivecache.localcache;

/**
 * 本地缓存客户端，缓存对象存储在当前运行的 JVM 内存中
 *
 * @author heimuheimu
 */
public interface NaiveLocalCacheClient {

    /**
     * 查找指定的 Key 在本地缓存中的值，如果不存在，则返回 {@code null}
     *
     * @param key 缓存 Key
     * @return Key 对应的缓存值，如果不存在，则返回 {@code null}
     */
    <T> T get(String key);

    /**
     * 将指定的对象存入本地缓存中
     *
     * @param key 缓存 Key
     * @param value 缓存对象
     * @param expiredTime 缓存过期时间，单位：秒
     */
    <T> void set(String key, T value, int expiredTime);

    /**
     * 将本地缓存中对应的 Key 删除
     *
     * @param key 缓存 Key
     */
    void delete(String key);
}
