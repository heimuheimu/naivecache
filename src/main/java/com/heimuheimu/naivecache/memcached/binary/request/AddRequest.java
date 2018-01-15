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
 * Memcached Add 命令请求包。命令定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#set-add-replace">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#set-add-replace
 * </a>
 *
 * @author heimuheimu
 */
public class AddRequest extends RequestPacket {

    private static final byte ADD_OPCODE = ByteUtil.intToByte(0x02);

    private final byte[] packet;

    /**
     * 构造一个 Memcached add 命令。
     *
     * @param key Key 值，必须存在，不允许为 {@code null} 或者为空
     * @param value Value 值，可以为 {@code null} 或者为空
     * @param expiry 过期时间，单位：秒，不允许小于 0
     * @param flags Flags 值，可以为 {@code null} 或者为空，如果存在，长度必须为4
     * @throws IllegalArgumentException Key 值为 {@code null} 或者为空
     * @throws IllegalArgumentException Expiry 值小于0
     * @throws IllegalArgumentException Flags 值存在，但长度不为4
     */
    public AddRequest(byte[] key, byte[] value, int expiry, byte[] flags)
            throws IllegalArgumentException {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Key could not be empty. Key: " +
                    Arrays.toString(key) + ", Value: " + Arrays.toString(value) +
                    ", Expiry: " + expiry + ", Flags: " + Arrays.toString(flags));
        }
        if (expiry < 0) {
            throw new IllegalArgumentException("Expiry could not less than 0. Key: " +
                    Arrays.toString(key) + ", Value: " + Arrays.toString(value) +
                    ", Expiry: " + expiry + ", Flags: " + Arrays.toString(flags));
        }
        if (flags != null && flags.length != 0 && flags.length != 4) {
            throw new IllegalArgumentException("Flags must be empty or 4 byte array. Key: " +
                    Arrays.toString(key) + ", Value: " + Arrays.toString(value) +
                    ", Expiry: " + expiry + ", Flags: " + Arrays.toString(flags));
        }
        byte[] extras = new byte[8];
        if (flags != null && flags.length == 4) {
            System.arraycopy(flags, 0, extras, 0, 4);
        }
        if (expiry > 0) {
            ByteUtil.intToFourByteArray(expiry, extras, 4);
        }
        this.packet = buildPacket(ADD_OPCODE, extras, key, value);
    }

    @Override
    public byte getOpcode() {
        return ADD_OPCODE;
    }

    @Override
    public byte[] getByteArray() {
        return packet;
    }
}
