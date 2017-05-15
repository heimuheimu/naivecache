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

package com.heimuheimu.naivecache.memcached.monitor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Memcached 命令执行次数统计信息
 * <p>当前实现是线程安全的</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public class OperationInfo {

    /**
     * 命令成功次数
     */
    private final AtomicLong success = new AtomicLong();

    /**
     * 命令未找到 Memcached Key 次数
     * <p><b>注意：</b>使用 multi-get 命令时，只要有一个 Key 未命中，也将被统计为未找到 Memcached Key 次数</p>
     */
    private final AtomicLong miss = new AtomicLong();

    /**
     * 命令超时次数
     */
    private final AtomicLong timeout = new AtomicLong();

    /**
     * 命令发生异常次数
     */
    private final AtomicLong error = new AtomicLong();

    /**
     * 增加一个命令执行次数统计
     *
     * @param result 命令执行结果
     */
    public void add(OperationResult result) {
        switch (result) {
            case SUCCESS:
                success.incrementAndGet();
                break;
            case MISS:
                miss.incrementAndGet();
                break;
            case TIMEOUT:
                timeout.incrementAndGet();
                break;
            case ERROR:
                error.incrementAndGet();
                break;
            default:
                throw new IllegalArgumentException("Unsupported OperationResult: `" + result + "`");
        }
    }

    /**
     * 获得命令成功次数
     *
     * @return 命令成功次数
     */
    public long getSuccess() {
        return success.get();
    }

    /**
     * 获得命令未找到 Memcached Key 次数
     * <p><b>注意：</b>使用 multi-get 命令时，只要有一个 Key 未命中，也将被统计为未找到 Memcached Key 次数</p>
     *
     * @return 命令未找到 Memcached Key 次数
     */
    public long getMiss() {
        return miss.get();
    }

    /**
     * 获得命令超时次数
     *
     * @return 命令超时次数
     */
    public long getTimeout() {
        return timeout.get();
    }

    /**
     * 获得命令发生异常次数
     *
     * @return 命令发生异常次数
     */
    public long getError() {
        return error.get();
    }

    @Override
    public String toString() {
        return "OperationInfo{" +
                "success=" + success +
                ", miss=" + miss +
                ", timeout=" + timeout +
                ", error=" + error +
                '}';
    }
}
