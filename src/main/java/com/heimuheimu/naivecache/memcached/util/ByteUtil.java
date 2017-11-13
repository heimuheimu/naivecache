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
 * 字节转换工具。
 *
 * @author heimuheimu
 */
public class ByteUtil {

	/**
	 * 将 0 - 255 范围内的正整数转换为 byte。
	 *
	 * @param value 需要被转换的 int 值
	 * @return byte 值
	 * @throws IllegalArgumentException 如果整数小于 0 或者大于 255
	 */
	public static byte intToByte(int value) throws IllegalArgumentException {
		if (value < 0 || value > 255) {
			throw new IllegalArgumentException("Invalid integer value: " + value);
		}
		return (byte) (value & 0xff);
	}

	/**
	 * 将 byte 转换为 0 - 255 范围的正整数。
	 *
	 * @param b 需要被转换的 byte 值
	 * @return 转换后的 int 值
	 */
	public static int byteToInt(byte b) {
		return b & 0xff;
	}

	/**
	 * 将 0 - 65535 范围的正整数转换为长度为 2 的 byte 数组，并写入目标数组的指定索引位置。
	 *
	 * @param value 需要被转换的 int 值
	 * @param src 目标数组
	 * @param offset 数组起始索引
	 * @throws IllegalArgumentException 如果整数小于 0 或者大于 65535
	 */
	public static void intToTwoByteArray(int value, byte[] src, int offset) throws IllegalArgumentException {
		if (value < 0 || value > 65535) {
			throw new IllegalArgumentException("Invalid integer value: " + value);
		}
		src[offset++] = (byte) ((value >>> 8) & 0xff);
		src[offset] = (byte) (value & 0xff);
	}

	/**
	 * 将长度为 2 的 byte 数组转换为 0 - 65535 范围的正整数。
	 *
	 * @param bytes 需要被转换的 byte 数组
	 * @param offset 数组起始索引
	 * @return 转换后的 int 值
	 * @throws IllegalArgumentException 当传入的数组长度不为 2
	 */
	public static int twoByteArrayToInt(byte[] bytes, int offset) {
		return ((bytes[offset++] & 0xff) << 8) | ((bytes[offset] & 0xff));
	}

	/**
	 * 将 int 转换为长度为 4 的 byte 数组，并写入目标数组的指定索引位置。
	 * 
	 * @param value 需要被转换的 int 值
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
	 * 将长度为 4 的 byte 数组转换为 int 后返回。
	 *
	 * @param bytes 需要被转换的 byte 数组
	 * @param offset 数组起始索引
	 * @return 转换后的 int 值
	 */
	public static int fourByteArrayToInt(byte[] bytes, int offset) {
		return (((bytes[offset++]) << 24) | ((bytes[offset++] & 0xff) << 16)
				| ((bytes[offset++] & 0xff) << 8) | ((bytes[offset] & 0xff)));
	}

	/**
	 * 将 long 转换为长度为 8 的 byte 数组，并写入目标数组的指定索引位置。
	 *
	 * @param value 需要被转换的 long 值
	 * @param src 目标数组
	 * @param offset 数组起始索引
	 */
	public static void longToEightByteArray(long value, byte[] src, int offset) {
		src[offset++] = (byte) (value >> 56);
		src[offset++] = (byte) (value >> 48);
		src[offset++] = (byte) (value >> 40);
		src[offset++] = (byte) (value >> 32);
		src[offset++] = (byte) (value >> 24);
		src[offset++] = (byte) (value >> 16);
		src[offset++] = (byte) (value >> 8);
		src[offset] = (byte) value;
	}

	/**
	 * 将长度为 8 的 byte 数组转换为 long 后返回。
	 *
	 * @param bytes 需要被转换的 byte 数组
	 * @param offset 数组起始索引
	 * @return 转换后的 long 值
	 */
	public static long eightByteArrayToLong(byte[] bytes, int offset) {
		return (((long) bytes[offset++]) << 56) | (((long) bytes[offset++] & 0xff) << 48)
				| (((long) bytes[offset++] & 0xff) << 40) | (((long) bytes[offset++] & 0xff) << 32)
				| (((long) bytes[offset++] & 0xff) << 24) | (((long) bytes[offset++] & 0xff) << 16)
				| (((long) bytes[offset++] & 0xff) << 8) | (((long) bytes[offset] & 0xff));
	}
}
