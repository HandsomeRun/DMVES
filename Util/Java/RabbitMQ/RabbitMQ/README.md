# RabbitMQ 的使用方法

## 基本配置

版本：V1.1
Editor：HandsomeRun
说明：V1.0 版本的路由器申明和队列申明太过于依赖配置文件，有不恰当的地方，将在下一个版本进行改进。
说明：V1.1 此版本修改了交换机和队列初始化，修改了 config.json 使得使用更加方便。
说明：V1.2 此版本进行了 `DMVESSenderMessage()` 和 `DMVESReceiverMessage()` 的方面方法提供

该文档仅包含 RabbitMQ 的软件包 `(./com.rabbitmq)` 和依赖库 `(./lib)`。

首先需要添加依赖包，可直接复制 `lib` 文件夹也可以自主添。分别是：

1. com.rabbitmq:qmqp-client:5.20.0
2. com.google.code.gson:gson:2.10.1

## config.json

本配置文件是明文存储的配置文件，主要包括以下内容：

1. `"rabbitmq"` ： rabbitmq 的基本配置

## Sender

为了简化整个 RabbitMQ 的软件包， Sender 仅包含**广播分发模式(fair.exchange)**和**公平分发模式(broadcast.exchange)**。

Sender 的主要任务是配置路由器 `exchange` ，主要修改 `name` 字段。

## Receiver

1. 公平分发模式的队列名应该在一格公平分法组保持一致，`routingKey` 应该一致。

## 举例使用

### Sender使用

```java
// 简单使用

Sender sender = new Sender() ; 
sender.DMVESSenderMessage(Sender.ControlName,Sender.TargetName , "hhhhhh");



// 自定义个性化使用
String broadcastExchange = "broadcast.exchange";
String faircastExchange = "fair.exchange" ;
String fairRoutingKey = "fair.routing.key" ;

Sender sender = new Sender();

sender.initExchange(broadcastExchange ,Sender.MQ_FANOUT ) ;
sender.initExchange(faircastExchange , Sender.MQ_DIRECT);

sender.sendBroadcastMessage(broadcastExchange , "这是一条广播消息");
sender.sendFairMessage(faircastExchange , fairRoutingKey ,"这是一条公平分发消息");

sender.close() ; 
```

### Receiver使用

```java
// 简单使用

Receiver receiver = new Receiver();
receiver.DMVESReceiverMessage(Receiver.ControlName, Receiver.TargetName, new MessageHandler() {
    @Override
    public void handleMessage(String message) {
        //  收到消息后，执行下面代码
        // ....
    }
});



// 自定义个性化使用
String broadcastExchange = "broadcast.exchange";
String faircastExchange = "fair.exchange" ;
String queueNameF = "fair.queue" ;
String fairRoutingKey = "fair.routing.key";


Receiver breceiver = new Receiver();
breceiver.initExchange(broadcastExchange , Receiver.MQ_FANOUT);

breceiver.receiveBroadcastMessage(broadcastExchange, new MessageHandler() {
    @Override
    public void handleMessage(String message) {
        System.out.println("[fanout] : " + message);
    }
});

Receiver freceiver = new Receiver();
freceiver.initExchange(faircastExchange , Receiver.MQ_DIRECT);
freceiver.initQueue(queueNameF);
freceiver.bindQueueToExchange(queueNameF,faircastExchange,fairRoutingKey);

freceiver.receiveFairMessage(faircastExchange, queueNameF, new MessageHandler() {
    @Override
    public void handleMessage(String message) {
        System.out.println("[fair] : " + message);
    }
});

```
