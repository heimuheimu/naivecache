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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * {@link Socket} 实例创建、配置信息读取、配置信息设置工具类。
 *
 * @author heimuheimu
 */
public class SocketBuilder {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(SocketBuilder.class);

	private SocketBuilder() {
		//prevent create instance
	}
	
	/**
	 * 根据目标主机地址（由主机名和端口组成，":" 符号分割，例如：localhost:4182）创建一个 {@link Socket} 实例，
	 * 使用 {@link SocketConfiguration#DEFAULT}  作为其配置信息。
	 *
	 * @param host 目标主机地址（由主机名和端口组成，":" 符号分割，例如：localhost:4182）
	 * @return {@code Socket} 实例
	 * @throws IllegalArgumentException 如果目标主机地址不符合规则，将会抛出此异常
	 * @throws BuildSocketException 如果在创建 {@code Socket} 过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String host) throws IllegalArgumentException, BuildSocketException {
		return create(host, null);
	}
	
	/**
	 * 根据目标主机地址（由主机名和端口组成，":" 符号分割，例如：localhost:4182） 创建一个 {@link Socket} 实例，
	 * 如果 {@code config} 为 {@code null}，将使用 {@link SocketConfiguration#DEFAULT}  作为其配置信息。
	 * 
	 * @param host 目标主机地址（由主机名和端口组成，":" 符号分割，例如：localhost:4182）
	 * @param config {@code Socket} 配置信息，允许为 {@code null}
	 * @return {@code Socket} 实例
	 * @throws IllegalArgumentException 如果目标主机地址不符合规则，将会抛出此异常
	 * @throws BuildSocketException 如果在创建 {@code Socket} 过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String host, SocketConfiguration config) throws IllegalArgumentException, BuildSocketException {
		String hostname;
		int port;
		try {
			String[] hostParts = host.split(":");
			hostname = hostParts[0];
			port = Integer.parseInt(hostParts[1]);
		} catch (Exception e) {
			LOGGER.error("Create socket failed: `invalid host`. Host: `" + host + "`. Config: `" + config + "`.", e);
			throw new IllegalArgumentException("Create socket failed: `invalid host`. Host: `" + host + "`. Config: `" + config + "`.", e);
		}
		return create(hostname, port, config);
	}
	
	/**
	 * 根据目标主机名、端口号创建一个 {@link Socket} 实例，使用 {@link SocketConfiguration#DEFAULT}  作为其配置信息。
	 * 
	 * @param hostname 目标主机名
	 * @param port 端口号
	 * @return {@code Socket} 实例
	 * @throws BuildSocketException 如果在创建 {@code Socket} 过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String hostname, int port) throws BuildSocketException {
		return create(hostname, port, null);
	}
	
	/**
	 * 根据目标主机名、端口号创建一个 {@link Socket} 实例，如果 {@code config} 为 {@code null}，
	 * 将使用 {@link SocketConfiguration#DEFAULT}  作为其配置信息。
	 * 
	 * @param hostname 目标主机名
	 * @param port 端口号
	 * @param config {@code Socket} 配置信息，允许为 {@code null}
	 * @return {@code Socket} 实例
	 * @throws BuildSocketException 如果在创建 {@code Socket} 过程中发生错误，将会抛出此异常
	 */
	public static Socket create(String hostname, int port, SocketConfiguration config) throws BuildSocketException {
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
			LOGGER.error("Create socket failed: `" + e.getMessage() + "`. Hostname: `"
					+ hostname + "`. Port: `" + port + "`. Config: `" + config + "`.", e);
			throw new BuildSocketException("Create socket failed: `" + e.getMessage() + "`. Hostname: `"
					+ hostname + "`. Port: `" + port + "`. Config: `" + config + "`.", e);
		}
	}
	
	/**
	 * 设置 {@link Socket} 配置信息，如果 {@code config} 为 {@code null}，将使用 {@link SocketConfiguration#DEFAULT}  作为其配置信息。
	 * 
	 * @param socket {@code Socket} 实例
	 * @param config {@code Socket} 配置信息，允许为 {@code null}
	 * @throws BuildSocketException 如果设置 {@link Socket} 配置信息过程中发生错误，将会抛出此异常
	 */
	public static void setConfig(Socket socket, SocketConfiguration config) throws BuildSocketException {
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
		} catch (Exception e) {
			LOGGER.error("Set socket config failed: `" + e.getMessage() + "`. Socket: `"
					+ socket + "`. Config: `" + config + "`.", e);
			throw new BuildSocketException("Set socket config failed: `" + e.getMessage() + "`. Socket: `"
					+ socket + "`. Config: `" + config + "`.", e);
		}
	}
	
	/**
	 * 读取 {@link Socket} 配置信息，返回的配置信息中不包含 {@link SocketConfiguration#getConnectionTimeout()} 配置项。
	 * 
	 * @param socket {@code Socket} 实例
	 * @return {@code Socket} 配置信息，不会返回 {@code null}
	 * @throws BuildSocketException 如果读取 {@link Socket} 配置信息过程中发生错误，将会抛出此异常
	 */
	public static SocketConfiguration getConfig(Socket socket) throws BuildSocketException {
		try {
		    SocketConfiguration config = new SocketConfiguration();
    		config.setKeepAlive(socket.getKeepAlive());
    		config.setTcpNoDelay(socket.getTcpNoDelay());
    		config.setSendBufferSize(socket.getSendBufferSize());
    		config.setReceiveBufferSize(socket.getReceiveBufferSize());
    		config.setSoTimeout(socket.getSoTimeout());
    		config.setSoLinger(socket.getSoLinger());
    		return config;
		} catch (Exception e) {
		    LOGGER.error("Get socket config failed: `" + e.getMessage() + "`. Socket: `" + socket + "`.", e);
		    throw new BuildSocketException("Get socket config failed: `" + e.getMessage() + "`. Socket: `" + socket + "`.", e);
		}
	}
	
}
