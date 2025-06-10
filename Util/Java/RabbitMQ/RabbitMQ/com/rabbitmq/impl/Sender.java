package com.rabbitmq.impl;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.config.MQConfigHelper;
import com.rabbitmq.config.RabbitMQConfig;
import com.rabbitmq.interfaces.ISender;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Sender implements ISender {
    public final static String MQ_FANOUT = "fanout" ;
    public final static String MQ_DIRECT = "direct" ;

    public final static String ControlName = "Controller" ;
    public final static String TargetName = "Target" ;
    public final static String NavigatorName = "Navigator" ;
    public final static String CarName = "Car" ;
    public final static String ViewName = "View";
    public final static String ExploreLogName = "ExploreLog";

    public final static String targetExchange       = "1.target.exchange";
    public final static String targetQueue          = "1.target.queue";
    public final static String targetRoutingKey     = "1.target.routing.key";
    public final static String navigatorExchange    = "1.navigator.exchange";
    public final static String navigatorQueue       = "1.navigator.queue";
    public final static String navigatorRoutingKey  = "1.navigator.routing.key";
    public final static String carExchange          = "1.car.exchange";
    public final static String viewExchange         = "1.view.exchange";
    public final static String exploreLogExchange   = "1.exploreLog.exchange";


    private Connection connection;
    private Channel channel;
    private final RabbitMQConfig config;

    public Sender() {
        this.config = MQConfigHelper.getInstance().getConfig();
        initConnection();
    }

    private void initConnection() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(config.getHost());
            factory.setPort(config.getPort());
            factory.setUsername(config.getUsername());
            factory.setPassword(config.getPassword());
            factory.setVirtualHost(config.getVirtualHost());
            factory.setConnectionTimeout(config.getConnectionTimeout());
            factory.setRequestedHeartbeat(config.getRequestedHeartbeat());

            connection = factory.newConnection();
            channel = connection.createChannel();

            // 使用 initExchange() 初始化交换机
            initExchange(carExchange, MQ_FANOUT);
            initExchange(viewExchange, MQ_FANOUT);
            initExchange(exploreLogExchange, MQ_FANOUT);
            initExchange(targetExchange, MQ_DIRECT);
            initExchange(navigatorExchange, MQ_DIRECT);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    @Override
    public void initExchange(String exchangeName, String exchangeType) {
        try {
            channel.exchangeDeclare(exchangeName, exchangeType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize exchange: " + exchangeName, e);
        }
    }

    @Override
    public void sendBroadcastMessage(String exchangeName, String message) {
        try {
            channel.basicPublish(exchangeName, "", null, message.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to send broadcast message", e);
        }
    }

    @Override
    public void sendFairMessage(String exchangeName, String routingKey , String message) {
        try {
            channel.basicPublish(exchangeName, routingKey, null, message.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to send fair message", e);
        }
    }

    public void DMVESSenderMessage(String scrName, String destName, String message) {
        switch (destName) {
            case TargetName:
                // Controller 发请求终点消息
                sendFairMessage(targetExchange, targetRoutingKey, message);
                break;
            case NavigatorName:
                // Controller 发请求导航消息
                sendFairMessage(navigatorExchange, navigatorRoutingKey, message);
                break;
            case CarName:
                // Controller 发小车移动消息（广播）
                sendBroadcastMessage(carExchange, message);
                break;
            case ViewName:
                // Controller 发View更新消息（广播）
                sendBroadcastMessage(viewExchange, message);
                break;
            case ExploreLogName:
                // Controller 或 Navigator 发 ExploreLog 记录消息（广播）
                sendBroadcastMessage(exploreLogExchange, message);
                break;
            default:
                throw new IllegalArgumentException("Unknown destName: " + destName);
        }
    }

    public void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to close RabbitMQ connection", e);
        }
    }
} 