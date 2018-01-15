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

package com.heimuheimu.naivecache.memcached.binary.request;

import com.heimuheimu.naivecache.memcached.util.ByteUtil;

import java.util.Arrays;

/**
 * Memcached Increment 命令请求包。命令定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#increment-decrement">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#increment-decrement
 * </a>
 *
 * @author heimuheimu
 */
public class IncrementRequest extends RequestPacket {

    private static final byte INCREMENT_OPCODE = ByteUtil.intToByte(0x05);

    private final byte[] packet;

    /**
     * 构造一个 Memcached increment 命令请求包。
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
    public IncrementRequest(byte[] key, long delta, long initialValue, int expiry) throws IllegalArgumentException {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Key could not be empty. Key: `" + Arrays.toString(key) + "`, Delta: `"
                    + delta + "`, InitialValue: `" + initialValue + "`, Expiry: `" + expiry + "`.");
        }
        if (delta < 0) {
            throw new IllegalArgumentException("Delta could not less than 0. Key: `" + Arrays.toString(key) + "`, Delta: `"
                    + delta + "`, InitialValue: `" + initialValue + "`, Expiry: `" + expiry + "`.");
        }
        if (initialValue < 0) {
            throw new IllegalArgumentException("InitialValue could not less than 0. Key: `" + Arrays.toString(key) + "`, Delta: `"
                    + delta + "`, InitialValue: `" + initialValue + "`, Expiry: `" + expiry + "`.");
        }
        if (expiry < 0) {
            throw new IllegalArgumentException("Expiry could not less than 0. Key: `" + Arrays.toString(key) + "`, Delta: `"
                    + delta + "`, InitialValue: `" + initialValue + "`, Expiry: `" + expiry + "`.");
        }
        byte[] extras = new byte[20];
        ByteUtil.longToEightByteArray(delta, extras, 0);
        ByteUtil.longToEightByteArray(initialValue, extras, 8);
        ByteUtil.intToFourByteArray(expiry, extras, 16);
        this.packet = buildPacket(INCREMENT_OPCODE, extras, key, null);
    }

    @Override
    public byte[] getByteArray() {
        return packet;
    }

    @Override
    public byte getOpcode() {
        return INCREMENT_OPCODE;
    }
}
