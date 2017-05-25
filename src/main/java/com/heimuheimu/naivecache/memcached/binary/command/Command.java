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

package com.heimuheimu.naivecache.memcached.binary.command;

import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.exception.MemcachedException;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;

import java.util.List;

/**
 * Memcached 命令，提供获取该命令的请求数据包、解析该命令的响应数据包等操作
 * <p>
 * <b>Memcached 命令的实现必须是线程安全的</b>
 * </p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public interface Command {

    /**
     * 获得该命令对应的请求数据包
     *
     * @return 该命令对应的请求数据包
     */
    byte[] getRequestByteArray();

    /**
     * {@link com.heimuheimu.naivecache.memcached.binary.channel.MemcachedChannel} 在发送完命令对应的请求数据包后，
     * 会通过该方法判断该命令是否需要继续接收响应数据包
     *
     * @return 该命令是否需要继续接收响应数据包
     * @see #receiveResponsePacket(ResponsePacket)
     */
    boolean hasResponsePacket();

    /**
     * 在 {@link #hasResponsePacket()} 方法返回 {@code true} 后，
     * {@link com.heimuheimu.naivecache.memcached.binary.channel.MemcachedChannel} 将会把下一个接收到响应数据包传入该方法
     * <p>
     * <b>注意：</b>当命令所对应的响应数据包接收完成后，{@link #hasResponsePacket()} 方法应返回 {@code false}
     * </p>
     *
     * @param responsePacket 响应数据包
     * @throws MemcachedException 当接收到非预期响应数据包时，抛出此异常
     * @see #hasResponsePacket()
     */
    void receiveResponsePacket(ResponsePacket responsePacket) throws MemcachedException;

    /**
     * 获得该命令对应的响应数据包列表
     * <p>该方法为阻塞式，调用后将会等待响应数据包全部到达</p>
     * <p><b>注意：</b>该方法不会返回 {@code null}，但有可能返回空列表</p>
     *
     * @param timeout 超时时间，单位：毫秒
     * @return 该命令对应的响应数据包列表
     * @throws TimeoutException 等待时间超过设置的超时时间，仍没有数据返回
     *
     */
    List<ResponsePacket> getResponsePacketList(long timeout) throws TimeoutException;

    /**
     * 关闭该命令，如果该命令处于等待响应数据包状态，应立刻释放
     */
    void close();

}
