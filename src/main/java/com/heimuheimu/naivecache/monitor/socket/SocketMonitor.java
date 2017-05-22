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

package com.heimuheimu.naivecache.monitor.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Socket 信息统计
 *
 * @author heimuheimu
 */
public class SocketMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(SocketMonitor.class);

    private static final SocketInfo GLOBAL_INFO = new SocketInfo("");

    private static final ConcurrentHashMap<String, SocketInfo> SOCKET_INFO_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    private SocketMonitor() {
        //private constructor
    }

    /**
     * 添加 Socket 单次读操作读取的字节长度统计
     * <p>注意：该方法不会抛出任何异常</p>
     *
     * @param host Socket 连接目标地址
     * @param size Socket 读字节长度
     */
    public static void addRead(String host, long size) {
        try {
            GLOBAL_INFO.addRead(size);
            SocketInfo socketInfo = get(host);
            socketInfo.addRead(size);
        } catch (Exception e) {
            //should not happen
            LOG.error("Unexpected error. Host: `" + host + "`, Size: `"
                    + size + "`.", e);
        }
    }

    /**
     * 添加 Socket 单次写操作写入的字节长度统计
     * <p>注意：该方法不会抛出任何异常</p>
     *
     * @param host Socket 连接目标地址
     * @param size Socket 写字节长度
     */
    public static void addWrite(String host, long size) {
        try {
            GLOBAL_INFO.addWrite(size);
            SocketInfo socketInfo = get(host);
            socketInfo.addWrite(size);
        } catch (Exception e) {
            //should not happen
            LOG.error("Unexpected error. Host: `" + host + "`, Size: `"
                    + size + "`.", e);
        }
    }

    public static Map<String, SocketInfo> get() {
        HashMap<String, SocketInfo> socketInfoHashMap = new HashMap<>(SOCKET_INFO_MAP);
        socketInfoHashMap.put("", GLOBAL_INFO);
        return socketInfoHashMap;
    }

    private static SocketInfo get(String host) {
        SocketInfo socketInfo = SOCKET_INFO_MAP.get(host);
        if (socketInfo == null) {
            synchronized (lock) {
                socketInfo = SOCKET_INFO_MAP.get(host);
                //noinspection Java8MapApi
                if (socketInfo == null) {
                    socketInfo = new SocketInfo(host);
                    SOCKET_INFO_MAP.put(host, socketInfo);
                }
            }
        }
        return socketInfo;
    }

}
