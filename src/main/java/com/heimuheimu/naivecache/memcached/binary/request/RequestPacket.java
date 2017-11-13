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

/**
 * Memcached 请求数据包抽象类，格式定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#packet-structure">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#packet-structure
 * </a>。
 *
 * @author heimuheimu
 */
public abstract class RequestPacket {

    private static final byte REQUEST_MAGIC_BYTE = ByteUtil.intToByte(0x80);

    /**
     * 构建 Memcached 请求数据包。
     *
     * @param Opcode 操作代码
     * @param extras Extras 字节数组，如果命令不需要 Extras ，该数组允许为空或者为 {@code null}
     * @param key Key 字节数组，如果命令不需要 Key，该数组允许为空或者为 {@code null}
     * @param value Value 字节数组，如果命令不需要 Value，该数组允许为空或者为 {@code null}
     * @return Memcached 请求数据包
     * @throws IllegalArgumentException 如果 Extras 长度超过255
     * @throws IllegalArgumentException 如果 Key 长度超过65535
     * @throws IllegalArgumentException 如果 Extras 长度 + Key 长度 + Value 长度超过2147483623
     */
    protected byte[] buildPacket(byte Opcode, byte[] extras, byte[] key, byte[] value) throws IllegalArgumentException {
        int extrasLength = extras != null ? extras.length : 0;
        if (extrasLength > 255) {
            throw new IllegalArgumentException("Extras is too large. Max extras length: 255. Invalid extras length: " + extrasLength);
        }
        int keyLength = key != null ? key.length : 0;
        if (keyLength > 65535) {
            throw new IllegalArgumentException("Key is too large. Max key length: 65535. Invalid key length: " + keyLength);
        }
        int valueLength = value != null ? value.length : 0;
        long totalBodyLength = extrasLength + keyLength + valueLength;
        if (totalBodyLength > 2147483623) {
            throw new IllegalArgumentException("Total body length is too large. Max length: 2147483623. Invalid length: " + totalBodyLength);
        }
        byte[] packet = new byte[24 + (int)totalBodyLength];
        packet[0] = REQUEST_MAGIC_BYTE;
        packet[1] = Opcode;
        if (keyLength > 0) {
            ByteUtil.intToTwoByteArray(keyLength, packet, 2);
            System.arraycopy(key, 0, packet, 24 + extrasLength, keyLength);
        }
        if (extrasLength > 0) {
            packet[4] = ByteUtil.intToByte(extrasLength);
            System.arraycopy(extras, 0, packet, 24 , extrasLength);
        }
        if (valueLength > 0) {
            System.arraycopy(value, 0, packet, 24 + extrasLength + keyLength, valueLength);
        }
        if (totalBodyLength > 0) {
            ByteUtil.intToFourByteArray((int) totalBodyLength, packet, 8);
        }
        return packet;
    }

    /**
     * 获得 Memcached 请求数据包的字节数组。
     *
     * @return Memcached 请求数据包的字节数组
     */
    public abstract byte[] getByteArray();

    /**
     * 获得请求操作代码，代码定义：
     * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#command-opcodes">
     * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#command-opcodes
     * </a>。
     *
     * @return 请求操作代码
     */
    public abstract byte getOpcode();
}
