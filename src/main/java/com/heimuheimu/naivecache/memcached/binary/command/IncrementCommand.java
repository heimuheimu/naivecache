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

import com.heimuheimu.naivecache.memcached.binary.request.IncrementRequest;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.exception.MemcachedException;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Memcached Increment 命令，命令定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#increment-decrement">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#increment-decrement
 * </a>。
 *
 * <p><strong>说明：</strong>{@code IncrementCommand} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class IncrementCommand implements Command {

    private final byte[] key;

    private final IncrementRequest incrementRequest;

    private final CountDownLatch latch = new CountDownLatch(1);

    private volatile boolean hasResponsePacket = true;

    private volatile ResponsePacket responsePacket;

    /**
     * 构造一个 Memcached Increment 命令。
     *
     * @param key Key 值，必须存在，不允许为 {@code null} 或者为空
     * @param delta 需要增加的值，不允许小于 0
     * @param initialValue 如果 Key 不存在，将会返回的初始值
     * @param expiry 过期时间，单位：秒，不允许小于 0
     * @throws IllegalArgumentException 如果 Key 值为 {@code null} 或者为空，将会抛出此异常
     * @throws IllegalArgumentException 如果 delta 小于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果 initialValue 小于 0，将会抛出此异常
     * @throws IllegalArgumentException 如果 expiry 小于 0，将会抛出此异常
     */
    public IncrementCommand(byte[] key, long delta, long initialValue, int expiry) throws IllegalArgumentException {
        this.incrementRequest = new IncrementRequest(key, delta, initialValue, expiry);
        this.key = key;
    }

    @Override
    public byte[] getRequestByteArray() {
        return incrementRequest.getByteArray();
    }

    @Override
    public boolean hasResponsePacket() {
        return hasResponsePacket;
    }

    @Override
    public void receiveResponsePacket(ResponsePacket responsePacket) throws MemcachedException {
        if (responsePacket.getOpcode() == incrementRequest.getOpcode()) {
            this.responsePacket = responsePacket;
            this.hasResponsePacket = false;
            latch.countDown();
        } else {
            //should not happen
            throw new MemcachedException("Increment command failed: `unexpected Opcode [" + responsePacket.getOpcode()
                    + "]`. Key: `" + Arrays.toString(key) + "`.");
        }
    }

    @Override
    public List<ResponsePacket> getResponsePacketList(long timeout) throws TimeoutException {
        boolean latchFlag;
        try {
            latchFlag = latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            latchFlag = false; //should not happen
        }
        if (latchFlag) {
            if (responsePacket != null) {
                return Collections.singletonList(responsePacket);
            } else {
                return new ArrayList<>();
            }
        } else {
            throw new TimeoutException("Wait increment command response timeout. Timeout:`" + timeout
                    + " ms`. `Key`: `" + Arrays.toString(key) + "`.");
        }
    }

    @Override
    public void close() {
        latch.countDown();
    }
}
