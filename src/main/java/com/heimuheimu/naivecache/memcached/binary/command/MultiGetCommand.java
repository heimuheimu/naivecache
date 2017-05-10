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

import com.heimuheimu.naivecache.memcached.binary.request.GetKQRequest;
import com.heimuheimu.naivecache.memcached.binary.request.GetKRequest;
import com.heimuheimu.naivecache.memcached.binary.request.RequestPacket;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.exception.MemcachedException;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Memcached 批量 get 命令，使用 getkq 和 getk 命令组合实现，实现方式请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#get-get-quietly-get-key-get-key-quietly">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#get-get-quietly-get-key-get-key-quietly
 * </a>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class MultiGetCommand implements Command {

    private final CountDownLatch latch = new CountDownLatch(1);

    private final byte[] requestPacket;

    private final byte[] lastKey;

    private volatile boolean hasResponsePacket = true;

    private List<ResponsePacket> responsePacketList = new ArrayList<>();

    private final Object responseLock = new Object();

    public MultiGetCommand(Collection<byte[]> keySet) {
        if (keySet == null || keySet.isEmpty()) {
            throw new IllegalArgumentException("Key set could not be empty: " + keySet);
        }
        ArrayList<RequestPacket> getRequestList = new ArrayList<>();
        Iterator<byte[]> keyIterator = keySet.iterator();
        int packetSize = 0;
        byte[] lastKey = null;
        while (keyIterator.hasNext()) {
            byte[] key = keyIterator.next();
            RequestPacket getRequest = null;
            if (keyIterator.hasNext()) {
                getRequest = new GetKQRequest(key);
            } else {
                getRequest = new GetKRequest(key);
                lastKey = key;
            }
            getRequestList.add(getRequest);
            packetSize += getRequest.toByteArray().length;
        }
        this.lastKey = lastKey;
        this.requestPacket = new byte[packetSize];
        int destPos = 0;
        for (RequestPacket getRequest : getRequestList) {
            byte[] getCommandPacket = getRequest.toByteArray();
            System.arraycopy(getCommandPacket, 0, requestPacket, destPos, getCommandPacket.length);
            destPos += getCommandPacket.length;
        }
    }

    @Override
    public byte[] toRequestPacket() {
        return requestPacket;
    }

    @Override
    public boolean hasResponsePacket() {
        return hasResponsePacket;
    }

    @Override
    public void receiveResponsePacket(ResponsePacket responsePacket) {
        synchronized (responseLock) {
            responsePacketList.add(responsePacket);
        }
        if (lastKey.length == responsePacket.getKeyLength()) {
            byte[] body = responsePacket.getBody();
            int keyEndIndex = responsePacket.getExtrasLength() + responsePacket.getKeyLength();
            boolean isLastKey = true;
            int lastKeyIndex = 0;
            for (int i = responsePacket.getExtrasLength(); i < keyEndIndex; i++) {
                if (lastKey[lastKeyIndex++] != body[i]) {
                    isLastKey = false;
                    break;
                }
            }
            if (isLastKey) {
                hasResponsePacket = false;
                latch.countDown();
            }
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
            synchronized (responseLock) {
                return responsePacketList;
            }
        } else {
            throw new TimeoutException("Wait mulit get command response timeout :" + timeout
                    + "ms. " + "Last key: " + Arrays.toString(lastKey));
        }
    }

    @Override
    public void close() {
        latch.countDown();
    }

}
