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

package com.heimuheimu.naivecache.memcached.listener;

import com.heimuheimu.naivecache.memcached.NaiveMemcachedClient;
import com.heimuheimu.naivecache.memcached.NaiveMemcachedClientListenerSkeleton;
import com.heimuheimu.naivecache.memcached.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 提供 Memcached 客户端事件监听器的一个简单实现，将 Error 和 SlowExecution 事件打印至日志文件中。
 * <p>
 * Log4j 配置：
 * <br>
 * <code>
 * log4j.logger.NAIVECACHE_ERROR_LOG=INFO, NAIVECACHE_ERROR_LOG <br>
 * log4j.additivity.NAIVECACHE_ERROR_LOG=false <br>
 * log4j.appender.NAIVECACHE_ERROR_LOG=org.apache.log4j.DailyRollingFileAppender <br>
 * log4j.appender.NAIVECACHE_ERROR_LOG.file=${log.output.directory}/naivecache/error.log <br>
 * log4j.appender.NAIVECACHE_ERROR_LOG.encoding=UTF-8 <br>
 * log4j.appender.NAIVECACHE_ERROR_LOG.DatePattern=_yyyy-MM-dd <br>
 * log4j.appender.NAIVECACHE_ERROR_LOG.layout=org.apache.log4j.PatternLayout <br>
 * log4j.appender.NAIVECACHE_ERROR_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n <br>
 * <br>
 * log4j.logger.NAIVECACHE_SLOW_EXECUTION_LOG=INFO, NAIVECACHE_SLOW_EXECUTION_LOG <br>
 * log4j.additivity.NAIVECACHE_SLOW_EXECUTION_LOG=false <br>
 * log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG=org.apache.log4j.DailyRollingFileAppender <br>
 * log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.file=${log.output.directory}/naivecache/slow_execution.log <br>
 * log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.encoding=UTF-8 <br>
 * log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.DatePattern=_yyyy-MM-dd <br>
 * log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.layout=org.apache.log4j.PatternLayout <br>
 * log4j.appender.NAIVECACHE_SLOW_EXECUTION_LOG.layout.ConversionPattern=%d{ISO8601} : %m%n <br>
 * </code>
 * </p>
 *
 * @author heimuheimu
 */
public class SimpleNaiveMemcachedClientListener extends NaiveMemcachedClientListenerSkeleton {

    private static final Logger NAIVECACHE_ERROR_LOG = LoggerFactory.getLogger("NAIVECACHE_ERROR_LOG");

    private static final Logger NAIVECACHE_SLOW_EXECUTION_LOG = LoggerFactory.getLogger("NAIVECACHE_SLOW_EXECUTION_LOG");

    @Override
    public void onError(NaiveMemcachedClient client, OperationType operationType, String key, String errorMessage) {
        NAIVECACHE_ERROR_LOG.error("`Key`:`{}`, `Op`:`{}`, `Host`:`{}`, `Error`:`{}`", key, operationType, client.getHost(), errorMessage);
    }

    @Override
    public void onSlowExecution(NaiveMemcachedClient client, OperationType operationType, String key, long executedNanoTime) {
        NAIVECACHE_SLOW_EXECUTION_LOG.info("`Key`:`{}`, `Op`:`{}`, `Cost`:`{}ns ({}ms)`, `Host`:`{}`", key, operationType,
                executedNanoTime, TimeUnit.MILLISECONDS.convert(executedNanoTime, TimeUnit.NANOSECONDS), client.getHost());
    }
}
