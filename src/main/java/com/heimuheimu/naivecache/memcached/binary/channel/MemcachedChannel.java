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

package com.heimuheimu.naivecache.memcached.binary.channel;

import com.heimuheimu.naivecache.constant.BeanStatusEnum;
import com.heimuheimu.naivecache.memcached.binary.command.Command;
import com.heimuheimu.naivecache.memcached.binary.command.OptimizedCommand;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacket;
import com.heimuheimu.naivecache.memcached.binary.response.ResponsePacketReader;
import com.heimuheimu.naivecache.memcached.exception.TimeoutException;
import com.heimuheimu.naivecache.memcached.monitor.SocketMonitorFactory;
import com.heimuheimu.naivecache.net.BuildSocketException;
import com.heimuheimu.naivecache.net.SocketBuilder;
import com.heimuheimu.naivecache.net.SocketConfiguration;
import com.heimuheimu.naivemonitor.monitor.SocketMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于 Memcached 二进制协议与 Memcached 服务进行数据交互的管道，协议定义请参考文档：
 * <a href="https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped">
 * https://github.com/memcached/memcached/wiki/BinaryProtocolRevamped
 * </a>。
 *
 * <p><strong>说明：</strong>{@code MemcachedChannel} 类是线程安全的，可在多个线程中使用同一个实例。</p>
 *
 * @author heimuheimu
 */
public class MemcachedChannel implements Closeable {

    private static final Logger MEMCACHED_CONNECTION_LOG = LoggerFactory.getLogger("NAIVECACHE_MEMCACHED_CONNECTION_LOG");

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedChannel.class);

    /**
     * Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     */
    private final String host;

    /**
     * 与 Memcached 服务器建立的 Socket 连接
     */
    private final Socket socket;

    /**
     * 当前数据交互管道使用的 Socket 信息监控器
     */
    private final SocketMonitor socketMonitor;

    /**
     * 当前实例所处状态
     */
    private volatile BeanStatusEnum state = BeanStatusEnum.UNINITIALIZED;

    /**
     * 等待发送的 Memcached 命令队列
     */
    private final LinkedBlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

    /**
     * IO线程
     */
    private IoTask ioTask = null;

    /**
     * 连续 {@link TimeoutException} 异常出现次数
     */
    private volatile long continuousTimeoutExceptionTimes = 0;

    /**
     * 最后一次出现 {@link TimeoutException} 异常的时间戳
     */
    private volatile long lastTimeoutExceptionTime = 0;

    /**
     * 创建基于 Memcached 二进制协议与 Memcached 服务进行数据交互的管道。
     *
     * @param host Memcached 地址，由主机名和端口组成，":"符号分割，例如：localhost:11211
     * @param configuration {@link Socket} 配置信息，如果传 {@code null}，将会使用 {@link SocketConfiguration#DEFAULT} 配置信息
     * @throws IllegalArgumentException 如果 Memcached 地址不符合规则，将会抛出此异常
     * @throws BuildSocketException 如果创建 {@link Socket} 过程中发生错误，将会抛出此异常
     */
    public MemcachedChannel(String host, SocketConfiguration configuration) throws IllegalArgumentException, BuildSocketException {
        this.host = host;
        this.socket = SocketBuilder.create(host, configuration);
        this.socketMonitor = SocketMonitorFactory.get(host);
    }

    public List<ResponsePacket> send(Command command, long timeout)
            throws NullPointerException, IllegalStateException, TimeoutException {
        if (command == null) {
            throw new NullPointerException("Memcached command could not be null. Host: `" + host + "`. " + socket);
        }
        if (state == BeanStatusEnum.NORMAL) {
            commandQueue.add(command);
        } else {
            throw new IllegalStateException("MemcachedChannel is not initialized or has been closed. State: `"
                    + state + "`. Host: `" + host + "`. " + socket);
        }
        try {
            return command.getResponsePacketList(timeout);
        } catch (TimeoutException e) {
            //如果两次超时异常发生在 1s 以内，则认为是连续失败
            if (System.currentTimeMillis() - lastTimeoutExceptionTime < 1000) {
                continuousTimeoutExceptionTimes ++;
            } else {
                continuousTimeoutExceptionTimes = 1;
            }
            lastTimeoutExceptionTime = System.currentTimeMillis();
            //如果连续超时异常出现次数大于 50 次，认为当前连接出现异常，关闭当前连接
            if (continuousTimeoutExceptionTimes > 50) {
                MEMCACHED_CONNECTION_LOG.error("MemcachedChannel need to be closed due to: `Too many timeout exceptions[{}]`. Host: `{}`.",
                        continuousTimeoutExceptionTimes, host);
                close();
            }
            throw e;
        }

    }

    public boolean isActive() {
        return state == BeanStatusEnum.NORMAL;
    }

    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            try {
                if (socket.isConnected() && !socket.isClosed()) {
                    state = BeanStatusEnum.NORMAL;
                    long startTime = System.currentTimeMillis();
                    SocketConfiguration config = SocketBuilder.getConfig(socket);
                    String socketAddress = host + "/" + socket.getLocalPort();
                    //启动写入线程
                    ioTask = new IoTask(config.getSendBufferSize());
                    ioTask.setName("naivecache-memcached-io-" + socketAddress);
                    ioTask.setDaemon(true);
                    ioTask.start();
                    MEMCACHED_CONNECTION_LOG.info("MemcachedChannel has been initialized. Cost: {}ms. Host: `{}`. Local port: `{}`. Config: `{}`.",
                            (System.currentTimeMillis() - startTime), host, socket.getLocalPort(), config);
                } else {
                    MEMCACHED_CONNECTION_LOG.error("Initialize MemcachedChannel failed. Socket is not connected or has been closed. Host: `{}`.", host);
                    close();
                }
            } catch(Exception e) {
                MEMCACHED_CONNECTION_LOG.error("Initialize MemcachedChannel failed. Unexpected error: `{}`. Host: `{}`.", e.getMessage(), host);
                LOG.error("Initialize MemcachedChannel failed. Unexpected error. Host: `" + host + "`. " + socket, e);
                close();
            }
        }
    }

    @Override
    public synchronized void close() {
        if (state != BeanStatusEnum.CLOSED) {
            long startTime = System.currentTimeMillis();
            state = BeanStatusEnum.CLOSED;
            try {
                //关闭Socket连接
                socket.close();
                //停止IO线程
                ioTask.stopSignal = true;
                ioTask.interrupt();
                MEMCACHED_CONNECTION_LOG.info("MemcachedChannel has been closed. Cost: {}ms. Host: `{}`.",
                        (System.currentTimeMillis() - startTime), host);
            } catch (Exception e) {
                MEMCACHED_CONNECTION_LOG.error("Close MemcachedChannel failed. Unexpected error: `{}`. Host: `{}`.", e.getMessage(), host);
                LOG.error("Close MemcachedChannel failed. Unexpected error. Host: `" + host + "`. " + socket, e);
            }
        }
    }

    @Override
    public String toString() {
        return "MemcachedChannel{" +
                "host='" + host + '\'' +
                ", socket=" + socket +
                ", state=" + state +
                ", continuousTimeoutExceptionTimes=" + continuousTimeoutExceptionTimes +
                ", lastTimeoutExceptionTime=" + lastTimeoutExceptionTime +
                '}';
    }

    private class IoTask extends Thread {

        private final ResponsePacketReader reader;

        private final int sendBufferSize;

        private volatile boolean stopSignal = false;

        private int mergedPacketSize = 0;

        private final ArrayList<Command> mergedCommandList = new ArrayList<>();

        /**
         * 等待响应数据包的 Memcached 命令队列
         */
        private final LinkedList<Command> waitingQueue = new LinkedList<>();

        public IoTask(Integer sendBufferSize) throws IOException {
            this.sendBufferSize = sendBufferSize != null ? sendBufferSize : 64 * 1024;
            this.reader = new ResponsePacketReader(socketMonitor, socket.getInputStream());
        }

        private void sendMergedPacket(OutputStream outputStream) throws IOException {
            if (mergedCommandList.size() > 1) {
                byte[] mergedPacket = new byte[mergedPacketSize];
                int destPos = 0;
                List<OptimizedCommand> sentOptimizedCommands = new ArrayList<>();
                for (Command command : mergedCommandList) {
                    boolean optimized = false;
                    if (command instanceof OptimizedCommand) {
                        for (OptimizedCommand sentOptimizedCommand : sentOptimizedCommands) {
                            if (sentOptimizedCommand.optimize((OptimizedCommand) command)) {
                                optimized = true;
                                break;
                            }
                        }
                    }
                    if (!optimized) {
                        byte[] commandPacket = command.getRequestByteArray();
                        System.arraycopy(commandPacket, 0, mergedPacket, destPos, commandPacket.length);
                        destPos += commandPacket.length;
                        if (command.hasResponsePacket()) {
                            waitingQueue.add(command);
                        }
                        if (command instanceof OptimizedCommand) {
                            sentOptimizedCommands.add((OptimizedCommand) command);
                        }
                    }
                }
                outputStream.write(mergedPacket, 0,  destPos);
                socketMonitor.onWritten(destPos);
                resetMergedPacket();
            } else if (mergedCommandList.size() == 1) {
                Command command = mergedCommandList.get(0);
                byte[] requestPacket = command.getRequestByteArray();
                outputStream.write(requestPacket);
                socketMonitor.onWritten(requestPacket.length);
                if (command.hasResponsePacket()) {
                    waitingQueue.add(command);
                }
                resetMergedPacket();
            }
        }

        private void addToMergedPacket(Command command) {
            mergedCommandList.add(command);
            mergedPacketSize += command.getRequestByteArray().length;
        }

        private void resetMergedPacket() {
            mergedCommandList.clear();
            mergedPacketSize = 0;
        }

        private void releaseWaitingCommand() {
            Command waitingCommand;
            while ((waitingCommand = waitingQueue.poll()) != null) {
                waitingCommand.close();
            }
        }

        @Override
        public void run() {
            try {
                OutputStream outputStream = socket.getOutputStream();
                Command command;
                while (!stopSignal) {
                    command = commandQueue.take();
                    if (command != null) {
                        byte[] requestPacket = command.getRequestByteArray();
                        if ((mergedPacketSize + requestPacket.length) < sendBufferSize) {
                            addToMergedPacket(command);
                            if (commandQueue.size() == 0) {
                                sendMergedPacket(outputStream);
                                outputStream.flush();
                            }
                        } else {
                            sendMergedPacket(outputStream);
                            if (commandQueue.size() == 0) {
                                outputStream.write(requestPacket);
                                socketMonitor.onWritten(requestPacket.length);
                                if (command.hasResponsePacket()) {
                                    waitingQueue.add(command);
                                }
                            } else {
                                addToMergedPacket(command);
                            }
                            outputStream.flush();
                        }
                    }
                    //如果该连接某个命令一直等待不到返回，可能会一直阻塞
                    while (waitingQueue.size() > 0) {
                        command = waitingQueue.peek();
                        ResponsePacket responsePacket = reader.read();
                        if (responsePacket != null) {
                            command.receiveResponsePacket(responsePacket);
                            if (!command.hasResponsePacket()) {
                                waitingQueue.poll();
                            }
                        } else {
                            MEMCACHED_CONNECTION_LOG.info("End of the input stream has been reached. Host: `{}`", host);
                            close();
                            releaseWaitingCommand();
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                releaseWaitingCommand();
            } catch (IOException e) {
                MEMCACHED_CONNECTION_LOG.error("[IoTask] MemcachedChannel need to be closed due to: `IOException: {}`. Host: `{}`.",
                        e.getMessage(), host);
                LOG.error("[IoTask] MemcachedChannel need to be closed: `IoException`. Host: `" + host + "`. " + socket, e);
                close();
                releaseWaitingCommand();
            } catch (Exception e) {
                MEMCACHED_CONNECTION_LOG.error("[IoTask] MemcachedChannel need to be closed due to: `{}`. Host: `{}`.",
                        e.getMessage(), host);
                LOG.error("[IoTask] MemcachedChannel need to be closed: `Unexpected error`. Host: `" + host + "`. " + socket, e);
                close();
                releaseWaitingCommand();
            }
        }

    }

}
