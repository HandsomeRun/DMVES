package com.rabbitmq.interfaces;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface ISender {
    /**
     * 发送广播消息
     * @param message 消息内容
     */
    void sendBroadcast(String message) throws IOException;

    /**
     * 发送公平分发消息
     * @param message 消息内容
     */
    void sendFairMessage(String message) throws IOException;

    /**
     * 关闭连接
     */
    void close() throws IOException, TimeoutException;
} 