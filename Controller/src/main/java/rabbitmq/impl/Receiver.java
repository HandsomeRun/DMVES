package rabbitmq.impl;

import com.rabbitmq.client.*;
import rabbitmq.config.MQConfigHelper;
import rabbitmq.config.RabbitMQConfig;
import rabbitmq.interfaces.IReceiver;
import rabbitmq.interfaces.MessageHandler;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Receiver implements IReceiver {
    public final static String MQ_FANOUT = "fanout" ;
    public final static String MQ_DIRECT = "direct" ;

    public final static String ControlName = "Controller" ;
    public final static String TargetName = "Target" ;
    public final static String NavigatorName = "Navigator" ;
    public final static String CarName = "Car" ;
    public final static String ViewName = "View";
    public final static String exporeLogName = "ExploreLog";

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

            // 使用 initExchange/initQueue/bindQueueToExchange 初始化
            initExchange(carExchange, MQ_FANOUT);
            initExchange(viewExchange, MQ_FANOUT);
            initExchange(exploreLogExchange, MQ_FANOUT);
            initExchange(targetExchange, MQ_DIRECT);
            initExchange(navigatorExchange, MQ_DIRECT);
            initQueue(targetQueue);
            bindQueueToExchange(targetQueue, targetExchange, targetRoutingKey);
            initQueue(navigatorQueue);
            bindQueueToExchange(navigatorQueue, navigatorExchange, navigatorRoutingKey);
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

    public void DMVESReceiverMessage(String scrName, String destName, MessageHandler handler) {
        switch (destName) {
            case TargetName:
                // Controller 发请求终点消息，监听 targetQueue
                receiveFairMessage(targetExchange, targetQueue, handler);
                break;
            case NavigatorName:
                // Controller 发请求导航消息，监听 navigatorQueue
                receiveFairMessage(navigatorExchange, navigatorQueue, handler);
                break;
            case CarName:
                // Controller 发小车移动消息（广播）
                receiveBroadcastMessage(carExchange, handler);
                break;
            case ViewName:
                // Controller 发View更新消息（广播）
                receiveBroadcastMessage(viewExchange, handler);
                break;
            case exporeLogName:
                // Controller 或 Navigator 发 ExploreLog 记录消息（广播）
                receiveBroadcastMessage(exploreLogExchange, handler);
                break;
            default:
                throw new IllegalArgumentException("Unknown destName: " + destName);
        }
    }
} 