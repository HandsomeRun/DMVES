package com.rabbitmq.impl;

import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.config.MQConfigHelper;
import com.rabbitmq.interfaces.ISender;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Sender implements ISender {
    private final Connection connection;
    private final Channel channel;
    private final MQConfigHelper configHelper;

    public Sender() throws IOException, TimeoutException {
        this.configHelper = MQConfigHelper.getInstance();
        this.connection = createConnection();
        this.channel = connection.createChannel();
        setupExchanges();
    }

    private Connection createConnection() throws IOException, TimeoutException {
        JsonObject config = configHelper.getRabbitMQConfig();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.get("host").getAsString());
        factory.setPort(config.get("port").getAsInt());
        factory.setUsername(config.get("username").getAsString());
        factory.setPassword(config.get("password").getAsString());
        factory.setVirtualHost(config.get("virtualHost").getAsString());
        factory.setConnectionTimeout(config.get("connectionTimeout").getAsInt());
        factory.setRequestedHeartbeat(config.get("requestedHeartbeat").getAsInt());
        return factory.newConnection();
    }

    private void setupExchanges() throws IOException {
        // 设置广播交换机
        JsonObject broadcastExchange = configHelper.getExchangeConfig("broadcast");
        channel.exchangeDeclare(
            broadcastExchange.get("name").getAsString(),
            broadcastExchange.get("type").getAsString(),
            broadcastExchange.get("durable").getAsBoolean()
        );

        // 设置公平分发交换机
        JsonObject fairExchange = configHelper.getExchangeConfig("fair");
        channel.exchangeDeclare(
            fairExchange.get("name").getAsString(),
            fairExchange.get("type").getAsString(),
            fairExchange.get("durable").getAsBoolean()
        );
    }

    @Override
    public void sendBroadcast(String message) throws IOException {
        JsonObject exchange = configHelper.getExchangeConfig("broadcast");
        channel.basicPublish(
            exchange.get("name").getAsString(),
            "", // 广播消息不需要路由键
            null,
            message.getBytes()
        );
    }

    @Override
    public void sendFairMessage(String message) throws IOException {
        JsonObject exchange = configHelper.getExchangeConfig("fair");
        String routingKey = configHelper.getRoutingKey("fair");
        channel.basicPublish(
            exchange.get("name").getAsString(),
            routingKey,
            null,
            message.getBytes()
        );
    }

    @Override
    public void close() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
} 