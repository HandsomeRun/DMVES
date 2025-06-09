package com.rabbitmq.impl;

import com.rabbitmq.client.*;
import com.rabbitmq.config.MQConfigHelper;
import com.rabbitmq.config.RabbitMQConfig;
import com.rabbitmq.interfaces.IReceiver;
import com.rabbitmq.interfaces.MessageHandler;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Receiver implements IReceiver {
    public final static String MQ_FANOUT = "fanout" ;
    public final static String MQ_DIRECT = "direct" ;

    private Connection connection;
    private Channel channel;
    private final RabbitMQConfig config;

    public Receiver() {
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
    public void initQueue(String queueName) {
        try {
            channel.queueDeclare(queueName, false, false, false, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize queue: " + queueName, e);
        }
    }

    @Override
    public void bindQueueToExchange(String queueName, String exchangeName, String routingKey) {
        try {
            channel.queueBind(queueName, exchangeName, routingKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to bind queue to exchange", e);
        }
    }

    @Override
    public String getQueueNameFromExchange(String exchangeName) {
        try {
            return channel.queueDeclare().getQueue();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get queue name from exchange", e);
        }
    }

    @Override
    public void receiveBroadcastMessage(String exchangeName, MessageHandler handler) {
        try {
            String queueName = getQueueNameFromExchange(exchangeName);
            bindQueueToExchange(queueName, exchangeName, "");
            setupConsumer(queueName, handler);
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup broadcast message receiver", e);
        }
    }

    @Override
    public void receiveFairMessage(String exchangeName, String queueName, MessageHandler handler) {
        try {
            setupConsumer(queueName, handler);
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup fair message receiver", e);
        }
    }

    private void setupConsumer(String queueName, MessageHandler handler) throws IOException {
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) {
                String message = new String(body);
                handler.handleMessage(message);
            }
        };
        channel.basicConsume(queueName, true, consumer);
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