package com.rabbitmq.interfaces;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface ISender {
    /**
     * 初始化交换机
     * @param exchangeName 交换机名称
     * @param exchangeType 交换机类型
     */
    void initExchange(String exchangeName, String exchangeType);

    /**
     * 发送广播消息
     * @param exchangeName 交换机名称
     * @param message 消息内容
     */
    void sendBroadcastMessage(String exchangeName, String message);

    /**
     * 发送公平分发消息
     * @param exchangeName 交换机名称
     * @param routingKey routingKey
     * @param message 消息内容
     */
    void sendFairMessage(String exchangeName,String routingKey, String message);

    /**
     * 发送 DMVES 业务消息
     * @param scrName 发送方名称
     * @param destName 接收方名称
     * @param message 消息内容
     */
    void DMVESSenderMessage(String scrName, String destName, String message);

    /**
     * 关闭连接
     */
    void close() throws IOException, TimeoutException;
} 