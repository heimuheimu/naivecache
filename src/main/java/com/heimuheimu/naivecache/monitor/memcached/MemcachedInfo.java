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

package com.heimuheimu.naivecache.monitor.memcached;

import com.heimuheimu.naivecache.monitor.ExecutionTimeInfo;
import com.heimuheimu.naivecache.monitor.TpsInfo;

/**
 * Memcached 统计信息
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class MemcachedInfo {

    /**
     * Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    private final String host;

    /**
     * Memcached TPS 统计信息
     */
    private final TpsInfo tpsInfo = new TpsInfo();

    /**
     * Memcached 命令执行时间统计信息
     */
    private final ExecutionTimeInfo executionTimeInfo = new ExecutionTimeInfo();

    /**
     * Memcached 所有命令执行次数统计信息
     */
    private final OperationInfo totalOpInfo = new OperationInfo();

    /**
     * Memcached get 命令次数统计信息
     */
    private final OperationInfo getOpInfo = new OperationInfo();

    /**
     * Memcached multi-get 命令次数统计信息
     */
    private final OperationInfo multiGetOpInfo = new OperationInfo();

    /**
     * Memcached set 命令次数统计信息
     */
    private final OperationInfo setOpInfo = new OperationInfo();

    /**
     * Memcached delete 命令次数统计信息
     */
    private final OperationInfo deleteOpInfo = new OperationInfo();

    /**
     * 构造一个 Memcached 统计信息
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    public MemcachedInfo(String host) {
        this.host = host;
    }

    /**
     * 新增一个 Memcached 命令请求统计
     *
     * @param op Memcached 命令类型
     * @param result Memcached 命令返回结果
     * @param startTime Memcached 命令请求开始时间(nanoTime)
     */
    public void add(OperationType op, OperationResult result, long startTime) {
        tpsInfo.add();
        executionTimeInfo.add(startTime);
        totalOpInfo.add(result);
        switch (op) {
            case GET:
                getOpInfo.add(result);
                break;
            case MULTI_GET:
                multiGetOpInfo.add(result);
                break;
            case SET:
                setOpInfo.add(result);
                break;
            case DELETE:
                deleteOpInfo.add(result);
                break;
            default:
                break;
        }
    }

    @Override
    public String toString() {
        return "MemcachedInfo{" +
                "host='" + host + '\'' +
                ", tpsInfo=" + tpsInfo +
                ", executionTimeInfo=" + executionTimeInfo +
                ", totalOpInfo=" + totalOpInfo +
                ", getOpInfo=" + getOpInfo +
                ", multiGetOpInfo=" + multiGetOpInfo +
                ", setOpInfo=" + setOpInfo +
                ", deleteOpInfo=" + deleteOpInfo +
                '}';
    }

}
