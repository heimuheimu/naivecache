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

package com.heimuheimu.naivecache.memcached;

/**
 * Memcached 客户端事件监听器抽象实现类，继承该类的监听器，仅需重载自己所关心的事件，
 * 可防止 {@link NaiveMemcachedClientListener} 在后续版本增加方法时，需重新调整监听器实现类。
 *
 * @author heimuheimu
 */
public abstract class NaiveMemcachedClientListenerSkeleton implements NaiveMemcachedClientListener {

    @Override
    public void onInvalidKey(NaiveMemcachedClient client, OperationType operationType, String key) {
        //do nothing
    }

    @Override
    public void onInvalidValue(NaiveMemcachedClient client, OperationType operationType, String key) {
        //do nothing
    }

    @Override
    public void onInvalidExpiry(NaiveMemcachedClient client, OperationType operationType, String key) {
        //do nothing
    }

    @Override
    public void onClosed(NaiveMemcachedClient client, OperationType operationType, String key) {
        //do nothing
    }

    @Override
    public void onKeyNotFound(NaiveMemcachedClient client, OperationType operationType, String key) {
        //do nothing
    }

    @Override
    public void onTimeout(NaiveMemcachedClient client, OperationType operationType, String key) {
        //do nothing
    }

    @Override
    public void onError(NaiveMemcachedClient client, OperationType operationType, String key, String errorMessage) {
        //do nothing
    }

    @Override
    public void onSlowExecution(NaiveMemcachedClient client, OperationType operationType, String key, long executedNanoTime) {
        //do nothing
    }

}
