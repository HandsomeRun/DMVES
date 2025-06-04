package com.rabbitmq.interfaces;

public interface MessageHandler {
    /**
     * 处理接收到的消息
     * @param message 消息内容
     */
    void handleMessage(String message);
} 