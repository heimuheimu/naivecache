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

import java.net.Socket;
import java.net.SocketOptions;

/**
 * {@link Socket} 配置信息。
 *
 * <p><strong>说明：</strong>{@code SocketConfiguration} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 * @see SocketBuilder
 */
public class SocketConfiguration {
	
	/**
	 * {@link Socket} 默认配置信息：
	 *
	 * <ul>
	 * <li>{@link SocketOptions#SO_KEEPALIVE} 值为 {@code true}</li>
	 * <li>{@link SocketOptions#SO_SNDBUF} 值为 32 KB</li>
	 * <li>{@link SocketOptions#SO_RCVBUF} 值为 32 KB</li>
	 * <li>{@code Socket} 连接超时时间值为 30 秒</li>
	 * </ul>
	 */
	public static final SocketConfiguration DEFAULT;
	
	static {
		SocketConfiguration config = new SocketConfiguration();
		config.setKeepAlive(true);
		config.setSendBufferSize(32 * 1024);
		config.setReceiveBufferSize(32 * 1024);
		config.setConnectionTimeout(30000);
		config.setConnectionTimeout(30000);
		config.setConnectionTimeout(30000);
		DEFAULT = config;
	}
	
	/**
	 * @see SocketOptions#SO_KEEPALIVE
	 */
	private volatile Boolean keepAlive = null;
	
	/**
	 * @see SocketOptions#TCP_NODELAY
	 */
	private volatile Boolean tcpNoDelay = null;
	
	/**
	 * @see SocketOptions#SO_SNDBUF
	 */
	private volatile Integer sendBufferSize = null;
	
	/**
	 * @see SocketOptions#SO_RCVBUF
	 */
	private volatile Integer receiveBufferSize = null;
	
	/**
	 * @see SocketOptions#SO_TIMEOUT
	 */
	private volatile Integer soTimeout = null;
	
	/**
	 * @see SocketOptions#SO_LINGER
	 */
	private volatile Integer soLinger = null;
	
	/**
	 * 建立 {@code Socket} 连接超时时间，单位：毫秒。 如果该值小于等于 0，则不设置连接超时时间
	 *
	 * @see Socket#connect(java.net.SocketAddress, int)
	 */
	private volatile int connectionTimeout = 0;

	/**
	 * 获得 {@link SocketOptions#SO_KEEPALIVE} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @return keepAlive 配置值，可能返回 {@code null}
	 */
	public Boolean getKeepAlive() {
		return keepAlive;
	}

	/**
	 * 设置 {@link SocketOptions#SO_KEEPALIVE} 配置值， 如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @param keepAlive keepAlive 配置值，允许为 {@code null}
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 */
	public void setKeepAlive(Boolean keepAlive) throws UnsupportedOperationException {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `keepAlive` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.keepAlive = keepAlive;
	}

	/**
	 * 获得 {@link SocketOptions#TCP_NODELAY} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @return tcpNoDelay 配置值，可能返回 {@code null}
	 */
	public Boolean getTcpNoDelay() {
		return tcpNoDelay;
	}

	/**
	 * 设置 {@link SocketOptions#TCP_NODELAY} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @param tcpNoDelay tcpNoDelay 配置值，允许为 {@code null}
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 */
	public void setTcpNoDelay(Boolean tcpNoDelay) throws UnsupportedOperationException {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `tcpNoDelay` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.tcpNoDelay = tcpNoDelay;
	}

	/**
	 * 获得 {@link SocketOptions#SO_SNDBUF} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @return sendBufferSize 配置值，可能返回 {@code null}
	 */
	public Integer getSendBufferSize() {
		return sendBufferSize;
	}

	/**
	 * 设置 {@link SocketOptions#SO_SNDBUF} 配置值， 如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @param sendBufferSize sendBufferSize 配置值，允许为 {@code null}
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 */
	public void setSendBufferSize(Integer sendBufferSize) throws UnsupportedOperationException {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `sendBufferSize` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.sendBufferSize = sendBufferSize;
	}

	/**
	 * 获得 {@link SocketOptions#SO_RCVBUF} 配置值，如果为 {@code null} ，则使用系统默认配置。
	 * 
	 * @return receiveBufferSize 配置值，可能返回 {@code null}
	 */
	public Integer getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * 设置 {@link SocketOptions#SO_RCVBUF} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @param receiveBufferSize receiveBufferSize 配置值，允许为 {@code null}
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 */
	public void setReceiveBufferSize(Integer receiveBufferSize) throws UnsupportedOperationException {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `receiveBufferSize` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.receiveBufferSize = receiveBufferSize;
	}

	/**
	 * 获得 {@link SocketOptions#SO_TIMEOUT} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @return soTimeout 配置值，可能返回 {@code null}
	 */
	public Integer getSoTimeout() {
		return soTimeout;
	}

	/**
	 * 设置 {@link SocketOptions#SO_TIMEOUT} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @param soTimeout soTimeout 配置值，允许为 {@code null}
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 */
	public void setSoTimeout(Integer soTimeout) throws UnsupportedOperationException {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `soTimeout` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.soTimeout = soTimeout;
	}

	/**
	 * 获得 {@link SocketOptions#SO_LINGER} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @return soLinger 配置值，可能返回 {@code null}
	 */
	public Integer getSoLinger() {
		return soLinger;
	}

	/**
	 * 设置 {@link SocketOptions#SO_LINGER} 配置值，如果为 {@code null}，则使用系统默认配置。
	 * 
	 * @param soLinger soLinger 配置值，允许为 {@code null}
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 */
	public void setSoLinger(Integer soLinger) {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `soLinger` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.soLinger = soLinger;
	}

	/**
	 * 获得建立 {@code Socket} 连接超时时间，单位：毫秒，如果该值小于等于0，则永远不会超时。
	 * 
	 * @return {@code Socket} 连接超时时间，单位：毫秒
	 * @see Socket#connect(java.net.SocketAddress, int)
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * 设置建立 {@code Socket} 连接超时时间，单位：毫秒，如果该值小于等于0，则永远不会超时。
	 * 
	 * @param connectionTimeout {@code Socket} 连接超时时间，单位：毫秒
	 * @throws UnsupportedOperationException 如果对 {@link #DEFAULT} 实例操作此方法，将会抛出此异常
	 * @see Socket#connect(java.net.SocketAddress, int)
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		if (this == DEFAULT) {
			throw new UnsupportedOperationException("Set `connectionTimeout` failed: `SocketConfiguration#DEFAULT could not be changed`.");
		}
		this.connectionTimeout = connectionTimeout;
	}

	@Override
	public String toString() {
		return "SocketConfiguration{" +
				"keepAlive=" + keepAlive +
				", tcpNoDelay=" + tcpNoDelay +
				", sendBufferSize=" + sendBufferSize +
				", receiveBufferSize=" + receiveBufferSize +
				", soTimeout=" + soTimeout +
				", soLinger=" + soLinger +
				", connectionTimeout=" + connectionTimeout +
				'}';
	}

}
