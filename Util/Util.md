# java Util的使用方法

## RedisUtil

### 基本信息

包含Car, CarAlgorithmEnum, CarStatusEnum, RedisUtil四个类. 除此之外,有一个可执行类Main,其中有使用代码示例.

考虑将除RedisUtil外的类整合成Model

### RedisUtil

单例类.

包名是cn.edu.ncepu, 若包名相同直接使用即可. 若报名不同,import即可

在源码中有每个方法的使用说明, 具体使用方法可见Main或者Controller

使用示例:
```java
//建立Redis连接
RedisUtil redisUtil = RedisUtil.getInstance();
try {
    redisUtil.getJedis(uuid);
} catch (Exception e) {
    System.out.println(e.getMessage());
    //写日志
}
```

### Car

定义了小车类和getter、setter. 小车属性详见README.md

### CarAlgorithmEnum

枚举了小车算法,算法详见README.md

### CarStatusEnum

枚举了小车状态,状态详见README.md


## RabbitMQ_util 的用法

具体见 `./Util/Java/RabbitMQ/RabbitMQ/README.md`。

软件包路径 `./Util/Java/RabbitMQ/RabbitMQ/com/*` + `./Util/Java/RabbitMQ/RabbitMQ/config.json`。

依赖路径 `./Util/Java/RabbitMQ/RabbitMQ/lib/*`

### Sender使用

```java
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

### 综合使用

#### 控制器给view发一个update

先确定模式
假如 控制器给view发一个update 的时候存在多个 view ，那就用广播模式

对于 Sender

```java
String exchange = "exchange.update" ;

Sender sender = new Sender() ;

// 先初始化
sender.initExchange( exchange ,Sender.MQ_FANOUT ) ;

while( true ) {
    
	sender.sendBroadcast（Messageexchange , "更新一次画面");
	
	Thread.sleep(100);
}

sender.close() ;
```

对于 Receiver

```java
String exchange = "exchange.update" ;

Receiver receiver = new Receiver() ;

// 初始化
receiver.initExchange(exchange , Receiver.MQ_FANOUT);

receiver.receiveBroadcastMessage(exchange ,new MessageHandler() {
    @Override
    public void handleMessage(String message) {
        // 我接受到了一个更新消息，我现在要更新画面
        update();
    }
});

```

#### 控制器给 Target 发一个 carID

先确定模式
假如 控制器给 Target 发一个 carID ，那就用公平分发

对于 Sender

```java
String exchange = "exchange.target" ;
String fairRoutingKey  = "target.routing.key";

Sender sender = new Sender() ;

// 先初始化
sender.initExchange(exchange ,Sender.MQ_DIRECT ) ;

while( true ) {
	sender.sendFairMessage(faircastExchange , fairRoutingKey ,"car1");
	
	Thread.sleep(100);
}

sender.close() ;
```

对于 Receiver

```java
String exchange = "exchange.target" ;
String fairRoutingKey  = "target.routing.key";
String queueName = "target.queue" ;

Receiver receiver = new Receiver() ;

// 初始化
receiver.initExchange(faircastExchange , Receiver.MQ_DIRECT);
receiver.initQueue(queueName);
receiver.bindQueueToExchange(queueName, exchange ,fairRoutingKey);

receiver.receiveFairMessage(exchange , queueName , new MessageHandler() {
    @Override
    public void handleMessage(String message) {
        // 我要给 message 里的小车寻找目标
        int carId = msgToCarId(message) ; 
        Position target = searchTargetForOneCar(carId) ; 
        // ... 
    }
});
```