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
import com.heimuheimu.naivecache.net.SocketBuilder;
import com.heimuheimu.naivecache.net.SocketConfiguration;
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
 * </a>
 * <p>当前实现是线程安全的</p>
 * @author heimuheimu
 * @ThreadSafe
 */
public class MemcachedChannel implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(MemcachedChannel.class);

    /**
     * 与 Memcached 服务器建立的Socket连接
     */
    private final Socket socket;

    /**
     * 当前实例所处状态
     * @see BeanStatusEnum
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

    public MemcachedChannel(Socket socket) {
        this.socket = socket;
    }

    public List<ResponsePacket> send(Command command, long timeout)
            throws NullPointerException, IllegalStateException, TimeoutException {
        if (command == null) {
            throw new NullPointerException("Memcached command could not be null. " + socket);
        }
        if (state == BeanStatusEnum.NORMAL) {
            commandQueue.add(command);
        } else {
            throw new IllegalStateException("MemcachedChannel is not initialized or has been closed. State: "
                    + state + ". " + socket);
        }
        return command.getResponsePacketList(timeout);
    }

    public synchronized void init() {
        if (state == BeanStatusEnum.UNINITIALIZED) {
            try {
                if (socket.isConnected() && !socket.isClosed()) {
                    state = BeanStatusEnum.NORMAL;
                    long startTime = System.currentTimeMillis();
                    SocketConfiguration config = SocketBuilder.getConfig(socket);
                    String socketAddress = socket.getInetAddress().getCanonicalHostName() + ":" + socket.getPort()
                            + "/" + socket.getLocalPort();
                    //启动写入线程
                    ioTask = new IoTask(config.getSendBufferSize());
                    ioTask.setName("[Memcached IO Thread] " + socketAddress);
                    ioTask.setDaemon(true);
                    ioTask.start();
                    //设置状态
                    LOG.info("MemcachedChannel has been initialized. Cost: {}ms. {}. {}",
                            (System.currentTimeMillis() - startTime), socket, config);
                } else {
                    LOG.error("Initialize MemcachedChannel failed. Socket is not connected or has been closed. {}", socket);
                    close();
                }
            } catch(Exception e) {
                LOG.error("Initialize MemcachedChannel failed. Unexpected error. " + socket, e);
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
                LOG.info("MemcachedChannel has been closed. Cost: {}ms. {}",
                        (System.currentTimeMillis() - startTime), socket);
            } catch (Exception e) {
                LOG.error("Close MemcachedChannel failed. Unexpected error. " + socket, e);
            }
        }
    }

    private class IoTask extends Thread {

        private final ResponsePacketReader reader;

        private final int sendBufferSize;

        private volatile boolean stopSignal = false;

        private int mergedPacketSize = 0;

        private ArrayList<Command> mergedCommandList = new ArrayList<>();

        /**
         * 等待响应数据包的 Memcached 命令队列
         */
        private final LinkedList<Command> waitingQueue = new LinkedList<>();

        public IoTask(Integer sendBufferSize) throws IOException {
            this.sendBufferSize = sendBufferSize != null ? sendBufferSize : 64 * 1024;
            this.reader = new ResponsePacketReader(socket.getInputStream());
        }

        private void sendMergedPacket(OutputStream outputStream) throws IOException {
            if (mergedCommandList.size() > 1) {
                byte[] mergedPacket = new byte[mergedPacketSize];
                int destPos = 0;
                List<OptimizedCommand> sendedOptimizedCommands = new ArrayList<>();
                for(int i = 0; i < mergedCommandList.size(); i++) {
                    Command command = mergedCommandList.get(i);
                    boolean optimized = false;
                    if (command instanceof OptimizedCommand) {
                        for (OptimizedCommand sendedOptimizedCommand : sendedOptimizedCommands) {
                            if (sendedOptimizedCommand.optimize((OptimizedCommand) command)) {
                                optimized = true;
                                break;
                            }
                        }
                    }
                    if (!optimized) {
                        byte[] commandPacket = command.toRequestPacket();
                        System.arraycopy(commandPacket, 0, mergedPacket, destPos, commandPacket.length);
                        destPos += commandPacket.length;
                        if (command.hasResponsePacket()) {
                            waitingQueue.add(command);
                        }
                        if (command instanceof OptimizedCommand) {
                            sendedOptimizedCommands.add((OptimizedCommand) command);
                        }
                    }
                }
                outputStream.write(mergedPacket, 0,  destPos);
                resetMergedPacket();
            } else if (mergedCommandList.size() == 1) {
                Command command = mergedCommandList.get(0);
                outputStream.write(command.toRequestPacket());
                if (command.hasResponsePacket()) {
                    waitingQueue.add(command);
                }
                resetMergedPacket();
            }
        }

        private void addToMergedPacket(Command command) {
            mergedCommandList.add(command);
            mergedPacketSize += command.toRequestPacket().length;
        }

        private void resetMergedPacket() {
            mergedCommandList.clear();
            mergedPacketSize = 0;
        }

        @Override
        public void run() {
            try {
                OutputStream outputStream = socket.getOutputStream();
                Command command;
                while (!stopSignal) {
                    command = commandQueue.take();
                    if (command != null) {
                        byte[] requestPacket = command.toRequestPacket();
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
                                if (command.hasResponsePacket()) {
                                    waitingQueue.add(command);
                                }
                            } else {
                                addToMergedPacket(command);
                            }
                            outputStream.flush();
                        }
                    }
                    //如果该连接某个命令一直等待不到返回，可能会一直阻塞，外部需正确处理连续TimeoutException，比如直接关闭
                    while (waitingQueue.size() > 0) {
                        command = waitingQueue.peek();
                        command.receiveResponsePacket(reader.read());
                        if (!command.hasResponsePacket()) {
                            waitingQueue.poll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                //因当前通道关闭才会抛出此异常，不做任何处理
            } catch (IOException e) {
                LOG.error("[IoTask] MemcachedChannel need to be closed due to: {}. {}", e.getMessage(), socket);
                close();
            } catch (Exception e) {
                LOG.error("[IoTask] Unexpected error. " + socket, e);
                close();
            }
        }

    }

}
