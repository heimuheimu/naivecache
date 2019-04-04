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

import com.heimuheimu.naivecache.memcached.binary.request.GetRequest;
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
 * Memcached Get 命令。命令定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#get-get-quietly-get-key-get-key-quietly">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#get-get-quietly-get-key-get-key-quietly
 * </a>
 *
 * <p><strong>说明：</strong>{@code GetCommand} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class GetCommand implements OptimizedCommand {

    private final byte[] key;

    private final GetRequest getRequest;

    private final CountDownLatch latch = new CountDownLatch(1);

    private volatile boolean hasResponsePacket = true;

    private volatile ResponsePacket responsePacket;

    private final List<GetCommand> optimizedCommandList = new ArrayList<>();

    private final Object optimizedLock = new Object();

    public GetCommand(byte[] key) {
        this.getRequest = new GetRequest(key);
        this.key = key;
    }

    @Override
    public byte[] getRequestByteArray() {
        return getRequest.getByteArray();
    }

    @Override
    public boolean hasResponsePacket() {
        return hasResponsePacket;
    }

    @Override
    public void receiveResponsePacket(ResponsePacket responsePacket) {
        if (responsePacket.getOpcode() == getRequest.getOpcode()) {
            this.responsePacket = responsePacket;
            this.hasResponsePacket = false;
            latch.countDown();
            synchronized (optimizedLock) {
                for (GetCommand optimizedCommand : optimizedCommandList) {
                    optimizedCommand.receiveResponsePacket(responsePacket);
                }
            }
        } else {
            //should not happen
            throw new MemcachedException("Get command failed. Unexpected Opcode: " + responsePacket.getOpcode()
                + ". Key: " + Arrays.toString(key));
        }
    }

    @Override
    public List<ResponsePacket> getResponsePacketList(long timeout) throws TimeoutException {
        boolean latchFlag;
        try {
            latchFlag = latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            latchFlag = false; //never happened
        }
        if (latchFlag) {
            if (responsePacket != null) {
                return Collections.singletonList(responsePacket);
            } else {
                return new ArrayList<>();
            }
        } else {
            throw new TimeoutException("Wait get command response timeout :" + timeout
                    + "ms. " + "Key: " + Arrays.toString(key));
        }
    }

    @Override
    public boolean optimize(OptimizedCommand target) {
        if (target instanceof GetCommand) {
            GetCommand targetGetCommand = (GetCommand) target;
            if (Arrays.equals(key, targetGetCommand.key)) {
                synchronized (optimizedLock) {
                    optimizedCommandList.add(targetGetCommand);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        latch.countDown();
    }
}
