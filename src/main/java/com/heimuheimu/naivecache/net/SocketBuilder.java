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
package com.heimuheimu.naivecache.net;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * {@link Socket}实例构造器
 *
 * @author heimuheimu
 */
public class SocketBuilder {
	
	/**
	 * 根据目标服务器地址（由主机名和端口组成，":"符号分割，例如：localhost:9610）生成{@link Socket}实例并返回。
	 * Socket配置信息使用{@link SocketConfiguration#DEFAULT}配置信息。
	 * <p>如果目标服务器地址不符合规则，将会抛出{@link IllegalArgumentException}异常
	 * <p>如果创建过程中发生错误，将会抛出{@link RuntimeException}异常
	 * 
	 * @param host 目标服务器地址
	 * @return Socket实例
	 * @throws IllegalArgumentException 如果目标服务器地址不符合规则，将会抛出此异常
	 * @throws RuntimeException 如果创建过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String host) 
			throws RuntimeException {
		return create(host, null);
	}
	
	/**
	 * 根据目标服务器地址（由主机名和端口组成，":"符号分割，例如：localhost:9610）、Socket配置信息生成{@link Socket}实例并返回。
	 * <p>如果目标服务器地址不符合规则，将会抛出{@link IllegalArgumentException}异常
	 * <p>如果创建过程中发生错误，将会抛出{@link RuntimeException}异常
	 * 
	 * @param host 目标服务器地址（由主机名和端口组成，":"符号分割，例如：localhost:9610）
	 * @param config Socket配置信息，如果传null，将会使用{@link SocketConfiguration#DEFAULT}配置信息
	 * @return Socket实例
	 * @throws IllegalArgumentException 如果目标服务器地址不符合规则，将会抛出此异常
	 * @throws RuntimeException 如果创建过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String host, SocketConfiguration config) 
			throws RuntimeException {
		String hostname = "";
		int port = -1;
		try {
			String[] hostParts = host.split(":");
			hostname = hostParts[0];
			port = Integer.parseInt(hostParts[1]);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid host: " + host, e);
		}
		return create(hostname, port, config);
	}
	
	/**
	 * 根据目标服务器主机名、端口号生成{@link Socket}实例并返回。Socket配置信息使用{@link SocketConfiguration#DEFAULT}配置信息。
	 * <p>如果创建过程中发生错误，将会抛出{@link RuntimeException}异常
	 * 
	 * @param hostname 目标服务器主机名
	 * @param port 端口号
	 * @return Socket实例
	 * @throws RuntimeException 如果创建过程中发生错误，将会抛出此异常
	 * @see #create(String, int, SocketConfiguration)
	 */
	public static Socket create(String hostname, int port) throws RuntimeException {
		return create(hostname, port, null);
	}
	
	/**
	 * 根据目标服务器主机名、端口号、Socket配置信息生成{@link Socket}实例并返回。
	 * <p>如果创建过程中发生错误，将会抛出{@link RuntimeException}异常
	 * 
	 * @param hostname 目标服务器主机名
	 * @param port 端口号
	 * @param config Socket配置信息，如果传null，将会使用{@link SocketConfiguration#DEFAULT}配置信息
	 * @return Socket实例
	 * @throws RuntimeException 如果创建过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String hostname, int port, SocketConfiguration config) 
		throws RuntimeException {
		try {
			if (config == null) {
				config = SocketConfiguration.DEFAULT;
			}
			Socket socket = new Socket();
			setConfig(socket, config);
			int connectionTimeout = config.getConnectionTimeout();
			if (connectionTimeout < 0) {
				connectionTimeout = 0;
			}
			socket.connect(new InetSocketAddress(hostname, port), connectionTimeout);
			return socket;
		} catch (Exception e) {
			throw new RuntimeException("Create socket failed. Hostname: " 
					+ hostname + ". Port: " + port + ". " + config);
		}
	}
	
	/**
	 * 根据Socket配置信息设置{@link Socket}实例，设置完后将其返回
	 * <p>如果设置过程中发生错误，将会抛出{@link RuntimeException}异常
	 * 
	 * @param socket Socket连接
	 * @param config Socket配置信息，如果传null，将会使用{@link SocketConfiguration#DEFAULT}配置信息
	 * @return Socket实例
	 * @throws RuntimeException 如果创建过程中发生错误，将会抛出此异常
	 */
	public static Socket setConfig(Socket socket, SocketConfiguration config) 
		throws RuntimeException {
		try {
			if (config == null) {
				config = SocketConfiguration.DEFAULT;
			}
			if (config.getKeepAlive() != null) {
				socket.setKeepAlive(config.getKeepAlive());
			}
			if (config.getTcpNoDelay() != null) {
				socket.setTcpNoDelay(config.getTcpNoDelay());
			}
			if (config.getSendBufferSize() != null) {
				socket.setSendBufferSize(config.getSendBufferSize());
			}
			if (config.getReceiveBufferSize() != null) {
				socket.setReceiveBufferSize(config.getReceiveBufferSize());
			}
			if (config.getSoTimeout() != null) {
				socket.setSoTimeout(config.getSoTimeout());
			}
			if (config.getSoLinger() != null && config.getSoLinger() > 0) {
				socket.setSoLinger(true, config.getSoLinger());
			}
			return socket;
		} catch (Exception e) {
			throw new RuntimeException("Set socket config failed. Socket: " 
					+ socket + ". Config: " + config);
		}
	}
	
	/**
	 * 获得该{@link Socket}实例的配置信息，但不包含{@link SocketConfiguration#getConnectionTimeout()}配置项
	 * <p>如果获取过程中发生错误，将会抛出{@link RuntimeException}异常
	 * 
	 * @param socket Socket连接
	 * @return Socket配置信息
	 * @throws SocketException 如果获取过程中发生错误，将会抛出此异常
	 */
	public static SocketConfiguration getConfig(Socket socket) throws SocketException {
		SocketConfiguration config = new SocketConfiguration();
		config.setKeepAlive(socket.getKeepAlive());
		config.setTcpNoDelay(socket.getTcpNoDelay());
		config.setSendBufferSize(socket.getSendBufferSize());
		config.setReceiveBufferSize(socket.getReceiveBufferSize());
		config.setSoTimeout(socket.getSoTimeout());
		config.setSoLinger(socket.getSoLinger());
		return config;
	}
	
}
