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
package com.heimuheimu.naivecache.memcached.util;

/**
 * 字节转换工具
 *
 * @author heimuheimu
 */
public class ByteUtil {

	/**
	 * 将0-255范围内的正整数转换为 byte
	 *
	 * @param value 需要被转换的int值
	 * @return byte值
	 * @throws IllegalArgumentException 如果整数小于0或者大于255
	 */
	public static byte intToByte(int value) throws IllegalArgumentException {
		if (value < 0 || value > 255) {
			throw new IllegalArgumentException("Invalid integer value: " + value);
		}
		return (byte) (value & 0xff);
	}

	/**
	 * 将byte转换为0-255范围的正整数
	 *
	 * @param b 需要被转换的byte值
	 * @return 转换后的int值
	 */
	public static int byteToInt(byte b) {
		return b & 0xff;
	}

	/**
	 * 将0-65535范围的正整数转换为长度为2的byte数组，并写入目标数组的指定索引位置
	 *
	 * @param value 需要被转换的int值
	 * @param src 目标数组
	 * @param offset 数组起始索引
	 * @throws IllegalArgumentException 如果整数小于0或者大于65535
	 */
	public static void intToTwoByteArray(int value, byte[] src, @SuppressWarnings("SameParameterValue") int offset) throws IllegalArgumentException {
		if (value < 0 || value > 65535) {
			throw new IllegalArgumentException("Invalid integer value: " + value);
		}
		src[offset++] = (byte) ((value >>> 8) & 0xff);
		src[offset] = (byte) (value & 0xff);
	}

	/**
	 * 将长度为2的byte数组转换为0-65535范围的正整数
	 *
	 * @param bytes 需要被转换的byte数组
	 * @param offset 数组起始索引
	 * @return 转换后的int值
	 * @throws IllegalArgumentException 当传入的数组长度不为2
	 */
	public static int twoByteArrayToInt(byte[] bytes, @SuppressWarnings("SameParameterValue") int offset) {
		return ((bytes[offset++] & 0xff) << 8) | ((bytes[offset] & 0xff));
	}

	/**
	 * 将int转换为长度为4的byte数组，并写入目标数组的指定索引位置
	 * 
	 * @param value 需要被转换的int值
	 * @param src 目标数组
	 * @param offset 数组起始索引
	 */
	public static void intToFourByteArray(int value, byte[] src, int offset) {
		src[offset++] = (byte) (value >> 24);
		src[offset++] = (byte) (value >> 16);
		src[offset++] = (byte) (value >> 8);
		src[offset] = (byte) value;
	}

	/**
	 * 将长度为4的byte数组转换为int后返回
	 *
	 * @param bytes 需要被转换的byte数组
	 * @param offset 数组起始索引
	 * @return 转换后的int值
	 */
	public static int fourByteArrayToInt(byte[] bytes, @SuppressWarnings("SameParameterValue") int offset) {
		return (((bytes[offset++]) << 24) | ((bytes[offset++] & 0xff) << 16)
				| ((bytes[offset++] & 0xff) << 8) | ((bytes[offset] & 0xff)));
	}

}
