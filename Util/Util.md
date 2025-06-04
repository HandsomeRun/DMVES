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
