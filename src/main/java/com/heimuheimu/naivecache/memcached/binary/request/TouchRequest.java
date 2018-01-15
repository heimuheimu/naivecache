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
 * Memcached Touch 命令请求包。命令定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#touch-gat-and-gatq">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#touch-gat-and-gatq
 * </a>
 *
 * @author heimuheimu
 */
public class TouchRequest extends RequestPacket {

    private static final byte TOUCH_OPCODE = ByteUtil.intToByte(0x1c);

    private final byte[] packet;

    /**
     * 构造一个 Memcached touch 命令。
     *
     * @param key Key 值，必须存在，不允许为 {@code null} 或者为空
     * @param expiry 过期时间，单位：秒，不允许小于 0
     * @throws IllegalArgumentException Key 值为 {@code null} 或者为空
     */
    public TouchRequest(byte[] key, int expiry) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Key could not be empty. Key: `" + Arrays.toString(key)
                    + "`, Expiry: `" + expiry + "`.");
        }
        if (expiry < 0) {
            throw new IllegalArgumentException("Expiry could not less than 0. Key: `" + Arrays.toString(key)
                    + "`, Expiry: `" + expiry + "`.");
        }
        byte[] extras = new byte[4];
        if (expiry > 0) {
            ByteUtil.intToFourByteArray(expiry, extras, 0);
        }
        packet = buildPacket(TOUCH_OPCODE, extras, key, null);
    }

    @Override
    public byte[] getByteArray() {
        return packet;
    }

    @Override
    public byte getOpcode() {
        return TOUCH_OPCODE;
    }
}
