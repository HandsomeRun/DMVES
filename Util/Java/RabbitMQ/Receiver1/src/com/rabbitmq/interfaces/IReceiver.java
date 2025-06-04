package com.rabbitmq.interfaces;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface IReceiver {
    /**
     * 接收广播消息
     * @param handler 消息处理器
     */
    void receiveBroadcast(MessageHandler handler) throws IOException;

    /**
     * 接收公平分发消息
     * @param handler 消息处理器
     */
    void receiveFairMessage(MessageHandler handler) throws IOException;

    /**
     * 关闭连接
     */
    void close() throws TimeoutException, IOException;
} 