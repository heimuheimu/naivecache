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

/**
 * <ul>
 * 使用单个 Memcached 服务的扩展客户端
 * <li>
 *     一次性 Memcached 客户端：{@link com.heimuheimu.naivecache.memcached.advance.OneTimeMemcachedClient}<br>
 *     <b>特性：</b>每次操作都会新建立 Socket 连接，在操作结束后关闭该连接。<br>
 *     <b>适用场景：</b> 连接单台 Memcached 服务器，Memcached 操作频次很低，例如几秒钟发起一次 Memcached 访问。
 * </li>
 * <li>
 *     自动重连 Memcached 客户端：{@link com.heimuheimu.naivecache.memcached.advance.AutoReconnectMemcachedClient}<br>
 *     <b>特性：</b>在 Memcached 操作之前均会检查客户端是否可用，如果不可用，则立即尝试重连<br>
 *     <b>适用场景：</b> 当只有单个 Memcached 服务可以使用时
 * </li>
 * </ul>
 *
 * @author heimuheimu
 */
package com.heimuheimu.naivecache.memcached.advance;