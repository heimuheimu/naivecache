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

package com.heimuheimu.naivecache.memcached.binary.transcoder;

/**
 * Java 对象与 Memcached 二进制协议存储的字节数组转换器
 * <p>该接口的实现必须保证线程安全</p>
 *
 * @author heimuheimu
 * @ThreadSafe
 */
public interface Transcoder {

    /**
     * 将 Java 对象编码成字节数组，并返回长度为2的二维数组，
     * 索引0的为 flags 字节数组，长度为4。索引1的为 value 字节数组，长度不固定
     *
     * @param value Java 对象
     * @return 长度为2的二维数组，索引0的为 flags 字节数组，长度为4，索引1的为 value 字节数组，长度不固定
     * @throws Exception 编码过程中发生错误
     */
    public byte[][] encode(Object value) throws Exception;

    /**
     * 将字节数组还原成 Java 对象并返回，如果当前转换器不支持该 flags 对应的转换，将会返回 {@code null}
     *
     * @param src 需要解码的字节数组
     * @param flagsOffset flags 在字节数组中的起始索引
     * @param valueOffset Value 在字节数组中的起始索引
     * @param valueLength Value 字节数组的长度
     * @return Java 对象
     * @throws Exception 解码过程中发生错误
     */
    public <T> T decode(byte[] src, int flagsOffset, int valueOffset, int valueLength) throws Exception;

}
