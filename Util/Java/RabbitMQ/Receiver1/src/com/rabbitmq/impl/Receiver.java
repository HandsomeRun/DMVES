package com.rabbitmq.impl;

import com.google.gson.JsonObject;
import com.rabbitmq.client.*;
import com.rabbitmq.config.MQConfigHelper;
import com.rabbitmq.interfaces.IReceiver;
import com.rabbitmq.interfaces.MessageHandler;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Receiver implements IReceiver {
    private final Connection connection;
    private final Channel channel;
    private final MQConfigHelper configHelper;
    private String fanoutQueueName = "" ;

    public Receiver() throws IOException, TimeoutException {
        this.configHelper = MQConfigHelper.getInstance();
        this.connection = createConnection();
        this.channel = connection.createChannel();
        setupQueues();
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

    private void setupQueues() throws IOException {
        // 设置广播队列
        JsonObject broadcastQueue = configHelper.getQueueConfig("broadcast");
        fanoutQueueName = broadcastQueue.get("name").getAsString() ;
        if(fanoutQueueName.equals( "" )) {
            fanoutQueueName = channel.queueDeclare().getQueue();
        }
        else {
            channel.queueDeclare(
                    fanoutQueueName,
                    broadcastQueue.get("durable").getAsBoolean(),
                    broadcastQueue.get("exclusive").getAsBoolean(),
                    broadcastQueue.get("autoDelete").getAsBoolean(),
                    null
            );
        }

        // 设置公平分发队列
        JsonObject fairQueue = configHelper.getQueueConfig("fair");
        channel.queueDeclare(
            fairQueue.get("name").getAsString(),
            fairQueue.get("durable").getAsBoolean(),
            fairQueue.get("exclusive").getAsBoolean(),
            fairQueue.get("autoDelete").getAsBoolean(),
            null
        );

        // 设置预取数量
        channel.basicQos(fairQueue.get("prefetchCount").getAsInt());

        // 绑定队列到交换机
        JsonObject broadcastExchange = configHelper.getExchangeConfig("broadcast");
        channel.queueBind(
                fanoutQueueName,
            broadcastExchange.get("name").getAsString(),
            ""
        );

        JsonObject fairExchange = configHelper.getExchangeConfig("fair");
        channel.queueBind(
            fairQueue.get("name").getAsString(),
            fairExchange.get("name").getAsString(),
            configHelper.getRoutingKey("fair")
        );
    }

    @Override
    public void receiveBroadcast(MessageHandler handler) throws IOException {
        JsonObject queue = configHelper.getQueueConfig("broadcast");
        channel.basicConsume(
            fanoutQueueName,
            true,
            new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                         AMQP.BasicProperties properties, byte[] body) {
                    String message = new String(body);
                    handler.handle(message);
                }
            }
        );
    }

    @Override
    public void receiveFairMessage(MessageHandler handler) throws IOException {
        JsonObject queue = configHelper.getQueueConfig("fair");
        channel.basicConsume(
            queue.get("name").getAsString(),
            false,
            new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                         AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body);
                    handler.handle(message);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        );
    }

    @Override
    public void close() throws TimeoutException, IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
} 