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
 * 提供各类连接单个 Memcached 服务的扩展客户端。
 *
 * <h3>一次性 Memcached 客户端</h3>
 * <blockquote>
 *     <strong>特性：</strong>每次操作都会新建立 Socket 连接，在操作结束后关闭该连接。<br>
 *     <strong>适用场景：</strong> 连接单台 Memcached 服务器，Memcached 操作频次很低，例如几秒钟发起一次 Memcached 访问。
 * </blockquote>
 *
 * <h3>自动重连 Memcached 客户端</h3>
 * <blockquote>
 *     <strong>特性：</strong>在 Memcached 操作之前均会检查客户端是否可用，如果不可用，则立即尝试重连。<br>
 *     <strong>适用场景：</strong> 连接单台 Memcached 服务器。
 * </blockquote>
 *
 * @author heimuheimu
 */
package com.heimuheimu.naivecache.memcached.advance;