package com.rabbitmq.interfaces;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.interfaces.MessageHandler;

public interface IReceiver {
    /**
     * 初始化交换机
     * @param exchangeName 交换机名称
     * @param exchangeType 交换机类型
     */
    void initExchange(String exchangeName, String exchangeType);

    /**
     * 初始化队列
     * @param queueName 队列名称
     */
    void initQueue(String queueName);

    /**
     * 绑定队列到交换机
     * @param queueName 队列名称
     * @param exchangeName 交换机名称
     * @param routingKey 路由键
     */
    void bindQueueToExchange(String queueName, String exchangeName, String routingKey);

    /**
     * 从交换机获取队列名
     * @param exchangeName 交换机名称
     * @return 队列名称
     */
    String getQueueNameFromExchange(String exchangeName);

    /**
     * 接收广播消息
     * @param exchangeName 交换机名称
     * @param handler 消息处理器
     */
    void receiveBroadcastMessage(String exchangeName, MessageHandler handler);

    /**
     * 接收公平分发消息
     * @param exchangeName 交换机名称
     * @param queueName 队列名称
     * @param handler 消息处理器
     */
    void receiveFairMessage(String exchangeName, String queueName, MessageHandler handler);

    /**
     * 关闭连接
     */
    void close() throws TimeoutException, IOException;
} 