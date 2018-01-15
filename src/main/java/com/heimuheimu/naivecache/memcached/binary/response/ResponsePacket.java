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

package com.heimuheimu.naivecache.memcached.binary.response;

import com.heimuheimu.naivecache.memcached.util.ByteUtil;

/**
 * Memcached 响应数据包。格式定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#packet-structure">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped#packet-structure
 * </a>
 *
 * <p><strong>注意：</strong> 响应数据包通常不应该被修改，否则无法保证线程安全。</p>
 *
 * <p><strong>说明：</strong>{@code ResponsePacket} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class ResponsePacket {

    private final byte[] header;

    private final byte[] body;

    private final int extrasLength;

    private final int keyLength;

    private final int valueLength;

    ResponsePacket(byte[] header, byte[] body) {
        this.header = header;
        this.body = body;
        if (body != null && body.length > 0) {
            extrasLength = ByteUtil.byteToInt(header[4]);
            keyLength = ByteUtil.twoByteArrayToInt(header, 2);
            valueLength = ByteUtil.fourByteArrayToInt(header, 8) - extrasLength - keyLength;
        } else {
            extrasLength = 0;
            keyLength = 0;
            valueLength = 0;
        }

    }

    public byte getOpcode() {
        return header[1];
    }

    public byte[] getHeader() {
        return header;
    }

    public byte[] getBody() {
        return body;
    }

    public int getExtrasLength() {
        return extrasLength;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public int getValueLength() {
        return valueLength;
    }

    public boolean isSuccess() {
        return header[6] == 0 && header[7] == 0;
    }

    public String getErrorMessage() {
        if (header[6] == 0) {
            switch (ByteUtil.byteToInt(header[7])) {
                case 0x00:
                    return "No error";
                case 0x01:
                    return "Key not found";
                case 0x02:
                    return "Key exists";
                case 0x03:
                    return "Value too large";
                case 0x04:
                    return "Invalid arguments";
                case 0x05:
                    return "Item not stored";
                case 0x06:
                    return "Incr/Decr on non-numeric value";
                case 0x07:
                    return "The vbucket belongs to another server";
                case 0x08:
                    return "Authentication error";
                case 0x09:
                    return "Authentication continue";
                case 0x81:
                    return "Unknown command";
                case 0x82:
                    return "Out of memory";
                case 0x83:
                    return "Not supported";
                case 0x84:
                    return "Internal error";
                case 0x85:
                    return "Busy";
                case 0x86:
                    return "Temporary failure";
            }
        }
        return "Unknown error. Status: " + header[6] + ", " + header[7];
    }

    public boolean isKeyNotFound() {
        return header[6] == 0 && header[7] == 1;
    }

}
