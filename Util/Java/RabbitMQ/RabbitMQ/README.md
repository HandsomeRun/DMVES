# RabbitMQ 的使用方法

## 基本配置

版本：V1.0
Editor：HandsomeRun
说明：V1.0 版本的路由器申明和队列申明太过于依赖配置文件，有不恰当的地方，将在下一个版本进行改进。

该文档仅包含 RabbitMQ 的软件包 `(./com.rabbitmq)` 和依赖库 `(./lib)`。

首先需要添加依赖包，可直接复制 `lib` 文件夹也可以自主添。分别是：

1. com.rabbitmq:qmqp-client:5.20.0
2. com.google.code.gson:gson:2.10.1

## config.json

本配置文件是明文存储的配置文件，主要包括以下内容：

1. `"rabbitmq"` ： rabbitmq 的基本配置
2. `"exchanges"` ： 交换机的基本配置
3. `"queues"` ： 队列的基本配置
4. `"routingKeys"` ： 公平分发的 key

## Sender

为了简化整个 RabbitMQ 的软件包， Sender 仅包含**广播分发模式(fair.exchange)**和**公平分发模式(broadcast.exchange)**。

Sender 的主要任务是配置路由器 `exchange` ，主要修改 `name` 字段。

## Receiver

1. 广播模式的广播队列名设置为 `""` 。
2. 公平分发模式的队列名应该在一格公平分法组保持一致。

## 举例使用

### Sender使用

```java
Sender sender = new Sender() ; 
sender.sendBroadcast("这是一条广播消息");
sender.sendFairMessage("这是一条公平分发消息" );
// 使用必要的 try-catch 包围即可
```

### Receiver使用

```java
Receiver receiver1 = new Receiver() ;

receiver1.receiveBroadcast(new MessageHandler() {
    @Override
    public void handle(String message) {
        // 接受到数据后的操作
        System.out.println("[fanout] : " + message);
    }
});

Receiver receiver2 = new Receiver();

receiver2.receiveFairMessage(new MessageHandler() {
    @Override
    public void handle(String message) {
        // 接受到数据后的操作
        System.out.println("[Fair] : " + message);
    }
});

// 使用必要的 try-catch 包围即可
```
