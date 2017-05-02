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

/**
 * 可优化的 Memcached 命令，如果目标命令允许被当前命令优化，则目标命令不会进行实际发送至 Memcached 服务，
 * 并将共享当前命令的响应数据包
 * <p>
 * <b>Memcached 命令的实现必须是线程安全的</b>
 * </p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public interface OptimizedCommand extends Command {

    /**
     * 当前命令是否可以对目标命令进行优化，如果可以，则返回 {@code true} , 否则返回 {@code false}
     *
     * @param target 目标命令
     * @return 是否可以对目标命令进行优化
     */
    public boolean optimize(OptimizedCommand target);

}
