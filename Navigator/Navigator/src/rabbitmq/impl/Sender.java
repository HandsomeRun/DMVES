package rabbitmq.impl;

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