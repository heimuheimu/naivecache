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
import com.heimuheimu.naivemonitor.monitor.SocketMonitor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Memcached 响应包读取器
 *
 * @author heimuheimu
 */
public class ResponsePacketReader {

    private static final byte RESPONSE_MAGIC_BYTE = ByteUtil.intToByte(0x81);

    private final SocketMonitor socketMonitor;

    private final InputStream inputStream;

    public ResponsePacketReader(SocketMonitor socketMonitor, InputStream inputStream) {
        this.socketMonitor = socketMonitor;
        this.inputStream = inputStream;
    }

    public ResponsePacket read() throws IOException {
        int headerPos = 0;
        byte[] header = new byte[24];
        byte[] body = null;
        while (headerPos < 24) {
            int readBytes = inputStream.read(header, headerPos, 24 - headerPos);
            socketMonitor.onRead(readBytes);
            if (readBytes >= 0) {
                headerPos += readBytes;
            } else {
                //流已经关闭，返回null
                return null;
            }
        }
        if (header[0] != RESPONSE_MAGIC_BYTE) {
            throw new IllegalStateException("Could not find response magic byte: 0x81. Actual byte: `" + header[0] + "`. Host: `"
                + socketMonitor.getHost() + "`.");
        }
        int bodyLength = ByteUtil.fourByteArrayToInt(header, 8);
        if (bodyLength > 0) {
            body = new byte[bodyLength];
            int bodyPos = 0;
            while (bodyPos < bodyLength) {
                int readBytes = inputStream.read(body, bodyPos, bodyLength - bodyPos);
                socketMonitor.onRead(readBytes);
                if (readBytes >= 0) {
                    bodyPos += readBytes;
                } else {
                    //流已经关闭，返回null
                    return null;
                }
            }
        }
        return new ResponsePacket(header, body);
    }

}
